/*
 *     Copyright 2015-2016 Austin Keener & Michael Ritter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.dv8tion.jda;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.almightyalpaca.discord.bot.system.util.NoteHubUploader;
import com.almightyalpaca.discord.bot.system.util.StringUtils;
import com.mashape.unirest.http.exceptions.UnirestException;

import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.MessageChannel;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.impl.MessageImpl;

public class MessageBuilder
{

    /**
     * Holds the Available formatting used in {@link #appendString(String, Formatting...)}
     */
    public enum Formatting
    {
        ITALICS("*"), BOLD("**"), STRIKETHROUGH("~~"), UNDERLINE("__"), BLOCK("`");

        private final String tag;

        Formatting(final String tag)
        {
            this.tag = tag;
        }

        private String getTag()
        {
            return this.tag;
        }

    }

    public abstract class SplitMode
    {
        protected abstract void send(MessageChannel channel) throws RuntimeException;
    }

    private final StringBuilder builder = new StringBuilder();
    private final List<User> mentioned = new LinkedList<>();
    private final List<TextChannel> mentionedTextChannels = new LinkedList<>();
    private boolean mentionEveryone = false;

    private boolean isTTS = false;

    public SplitMode SPIT = new SplitMode()
    {

        @Override
        protected void send(final MessageChannel channel)
        {
            final String message = MessageBuilder.this.builder.toString();
            if (message.isEmpty())
            {
                throw new UnsupportedOperationException("Cannot build a Message with no content. (You never added any content to the message)");
            }

            final String[] messages = StringUtils.split(message, 2000, "\n");

            final List<MessageImpl> msgs = new ArrayList<>(messages.length);

            for (final String string : messages)
            {
                msgs.add(new MessageImpl("", null).setContent(string).setTTS(MessageBuilder.this.isTTS).setMentionedUsers(MessageBuilder.this.mentioned)
                        .setMentionedChannels(MessageBuilder.this.mentionedTextChannels).setMentionsEveryone(MessageBuilder.this.mentionEveryone));
            }

            for (final MessageImpl msg : msgs)
            {
                channel.sendMessage(msg);
            }
        }
    };

    public SplitMode UPLOAD = new SplitMode()
    {

        @Override
        protected void send(final MessageChannel channel) throws RuntimeException
        {
            this.withMessage("The Message was too long, you can view it here: $url$").send(channel);
        }

        public SplitMode withMessage(final String message)
        {
            return new SplitMode()
            {
                @Override
                protected void send(final MessageChannel channel) throws RuntimeException
                {
                    try
                    {
                        final URL url = NoteHubUploader.upload(MessageBuilder.this.getMessage(), MessageBuilderSettings.notehubPassword);
                        channel.sendMessage(message.replace("$url$", url.toString()));
                    }
                    catch (IOException | UnirestException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
    };

    public SplitMode AUTO = new SplitMode()
    {

        @Override
        protected void send(final MessageChannel channel) throws RuntimeException
        {
            try
            {
                if (StringUtils.split(MessageBuilder.this.getMessage(), 2000, "\n").length <= 4)
                {
                    MessageBuilder.this.SPIT.send(channel);
                }
                else
                {
                    MessageBuilder.this.UPLOAD.send(channel);
                }
            }
            catch (final Exception e)
            {
                MessageBuilder.this.UPLOAD.send(channel);
            }
        }
    };

    public SplitMode TRUNCATE = new SplitMode()
    {
        @Override
        protected void send(final MessageChannel channel)
        {
            channel.sendMessage(
                    new MessageImpl("", null).setContent(MessageBuilder.this.getMessage().substring(0, Math.min(MessageBuilder.this.getLength(), 1997)) + "...")
                            .setTTS(MessageBuilder.this.isTTS).setMentionedUsers(MessageBuilder.this.mentioned)
                            .setMentionedChannels(MessageBuilder.this.mentionedTextChannels).setMentionsEveryone(MessageBuilder.this.mentionEveryone));

        }

    };

    /**
     * Appends a code-block to the Message
     *
     * @param text
     *        the code to append
     * @param language
     *        the language of the code. If unknown use an empty string
     * @return this instance
     */
    public MessageBuilder appendCodeBlock(final String text, final String language)
    {
        this.builder.append("```").append(language).append('\n').append(text).append("\n```");
        return this;
    }

    /**
     * Appends a @everyone mention to the Message
     *
     * @return this instance
     */
    public MessageBuilder appendEveryoneMention()
    {
        this.builder.append("@everyone");
        this.mentionEveryone = true;
        return this;
    }

    /**
     * Appends a channel mention to the Message. For this to work, the given TextChannel has to be from the Guild the mention is posted to.
     *
     * @param channel
     *        the TextChannel to mention
     * @return this instance
     */
    public MessageBuilder appendMention(final TextChannel channel)
    {
        this.builder.append("<#").append(channel.getId()).append('>');
        this.mentionedTextChannels.add(channel);
        return this;
    }

    /**
     * Appends a mention to the Message
     *
     * @param user
     *        the user to mention
     * @return this instance
     */
    public MessageBuilder appendMention(final User user)
    {
        this.builder.append("<@").append(user.getId()).append('>');
        this.mentioned.add(user);
        return this;
    }

    /**
     * Appends a string to the Message
     *
     * @param text
     *        the text to append
     * @return this instance
     */
    public MessageBuilder appendString(final String text)
    {
        this.builder.append(text);
        return this;
    }

    /**
     * Appends a formatted string to the Message
     *
     * @param text
     *        the text to append
     * @param format
     *        the format(s) to apply to the text
     * @return this instance
     */
    public MessageBuilder appendString(final String text, final Formatting... format)
    {
        boolean blockPresent = false;
        for (final Formatting formatting : format)
        {
            if (formatting == Formatting.BLOCK)
            {
                blockPresent = true;
                continue;
            }
            this.builder.append(formatting.getTag());
        }
        if (blockPresent)
        {
            this.builder.append(Formatting.BLOCK.getTag());
        }

        this.builder.append(text);

        if (blockPresent)
        {
            this.builder.append(Formatting.BLOCK.getTag());
        }
        for (int i = format.length - 1; i >= 0; i--)
        {
            if (format[i] == Formatting.BLOCK)
            {
                continue;
            }
            this.builder.append(format[i].getTag());
        }
        return this;
    }

    /**
     * Creates a {@link net.dv8tion.jda.entities.Message Message} object from this Builder
     *
     * @return the created {@link net.dv8tion.jda.entities.Message Message}
     * @throws java.lang.UnsupportedOperationException
     *         <ul>
     *         <li>If you attempt to build() an empty Message (no content added to the Message)</li>
     *         <li>If you attempt to build() a Message with more than 2000 characters of content.</li>
     *         </ul>
     */
    public Message build()
    {
        final String message = this.builder.toString();
        if (message.isEmpty())
        {
            throw new UnsupportedOperationException("Cannot build a Message with no content. (You never added any content to the message)");
        }
        if (message.length() > 2000)
        {
            throw new UnsupportedOperationException("Cannot build a Message with more than 2000 characters. Please limit your input.");
        }

        return new MessageImpl("", null).setContent(message).setTTS(this.isTTS).setMentionedUsers(this.mentioned)
                .setMentionedChannels(this.mentionedTextChannels).setMentionsEveryone(this.mentionEveryone);
    }

    /**
     * Returns the current length of the content that will be built into a {@link net.dv8tion.jda.entities.Message Message} when {@link #build()} is called.<br>
     * If this value is <code>0</code> or greater than <code>2000</code> when {@link #build()} is called, an exception will be raised.
     *
     * @return The currently length of the content that will be built into a Message.
     */
    public int getLength()
    {
        return this.builder.length();
    }

    /**
     * Returns the current message String
     *
     * @return the message
     */
    public String getMessage()
    {
        return this.builder.toString();
    }

    /**
     * Appends a new line char (\n) to the Message
     *
     * @return this instance
     */
    public MessageBuilder newLine()
    {
        this.builder.append("\n");
        return this;
    }

    public void send(final MessageChannel channel) throws RuntimeException
    {
        this.send(channel, this.AUTO);
    }

    public void send(final MessageChannel channel, final SplitMode mode) throws RuntimeException
    {
        mode.send(channel);
    }

    /**
     * Makes the created Message a TTS message
     *
     * @param tts
     *        whether the created Message should be a tts message
     * @return this instance
     */
    public MessageBuilder setTTS(final boolean tts)
    {
        this.isTTS = tts;
        return this;
    }
}