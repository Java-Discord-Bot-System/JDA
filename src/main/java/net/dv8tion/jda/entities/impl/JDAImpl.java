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
package net.dv8tion.jda.entities.impl;

import com.mashape.unirest.http.Unirest;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.*;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.guild.GuildJoinEvent;
import net.dv8tion.jda.hooks.EventListener;
import net.dv8tion.jda.hooks.IEventManager;
import net.dv8tion.jda.hooks.InterfacedEventManager;
import net.dv8tion.jda.hooks.SubscribeEvent;
import net.dv8tion.jda.managers.AccountManager;
import net.dv8tion.jda.managers.AudioManager;
import net.dv8tion.jda.managers.GuildManager;
import net.dv8tion.jda.managers.impl.AudioManagerImpl;
import net.dv8tion.jda.requests.Requester;
import net.dv8tion.jda.requests.WebSocketClient;
import net.dv8tion.jda.utils.SimpleLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * Represents the core of the Discord API. All functionality is connected through this.
 */
public class JDAImpl implements JDA
{
    public static final SimpleLog LOG = SimpleLog.getLog("JDA");
    private final HttpHost proxy;
    private final Map<String, User> userMap = new HashMap<>();
    private final Map<String, Guild> guildMap = new HashMap<>();
    private final Map<String, TextChannel> textChannelMap = new HashMap<>();
    private final Map<String, VoiceChannel> voiceChannelMap = new HashMap<>();
    private final Map<String, PrivateChannel> pmChannelMap = new HashMap<>();
    private final Map<String, String> offline_pms = new HashMap<>();    //Userid -> channelid
    private final Map<Guild, AudioManager> audioManagers = new HashMap<>();
    private final boolean audioEnabled;
    private final boolean useShutdownHook;
    private IEventManager eventManager = new InterfacedEventManager();
    private SelfInfo selfInfo = null;
    private AccountManager accountManager;
    private String authToken = null;
    private WebSocketClient client;
    private final Requester requester = new Requester(this);
    private boolean reconnect;
    private int responseTotal;
    private Long messageLimit = null;

    public JDAImpl(boolean enableAudio, boolean useShutdownHook)
    {
        proxy = null;
        if (enableAudio)
            this.audioEnabled = AudioManagerImpl.init();
        else
            this.audioEnabled = false;
        this.useShutdownHook = useShutdownHook;
    }

    public JDAImpl(String proxyUrl, int proxyPort, boolean enableAudio, boolean useShutdownHook)
    {
        if (proxyUrl == null || proxyUrl.isEmpty() || proxyPort == -1)
            throw new IllegalArgumentException("The provided proxy settings cannot be used to make a proxy. Settings: URL: '" + proxyUrl + "'  Port: " + proxyPort);
        proxy = new HttpHost(proxyUrl, proxyPort);
        Unirest.setProxy(proxy);
        if (enableAudio)
            this.audioEnabled = AudioManagerImpl.init();
        else
            this.audioEnabled = false;
        this.useShutdownHook = useShutdownHook;
    }

    /**
     * Attempts to login to Discord with a Bot-Account.
     *
     * @param token
     *          The token of the bot-account attempting to log in.
     * @param sharding
     *          A array of length 2 used for sharding or null. Refer to JDABuilder#useSharding for more details
     * @throws IllegalArgumentException
     *          Thrown if: <ul>
     *              <li>the botToken provided is empty or null.</li>
     *              <li>The sharding parameter is invalid.</li>
     *          </ul>
     * @throws LoginException
     *          Thrown if the token fails the auth check with the Discord servers.
     */
    public void login(String token, int[] sharding) throws IllegalArgumentException, LoginException
    {
        LOG.info("JDA starting...");
        if (token == null || token.isEmpty())
            throw new IllegalArgumentException("The provided botToken was empty / null.");
        if (sharding != null && (sharding.length != 2 || sharding[0] < 0 || sharding[0] >= sharding[1] || sharding[1] < 2))
            throw new IllegalArgumentException("Sharding array is wrong. please refer to JDABuilder#useSharding for help");

        accountManager = new AccountManager(this);

        if(!validate(token)) {
            throw new LoginException("The given token was invalid");
        }

        LOG.info("Login Successful!");
        client = new WebSocketClient(this, proxy, sharding);
        client.setAutoReconnect(reconnect);


        if (useShutdownHook)
        {
            Runtime.getRuntime().addShutdownHook(new Thread()
            {
                @Override
                public void run()
                {
                    JDAImpl.this.shutdown();
                }
            });
        }
    }

    private boolean validate(String authToken)
    {
        this.authToken = authToken;
        try
        {
            if (getRequester().get(Requester.DISCORD_API_PREFIX + "users/@me/guilds").isOk())
            {
                return true;
            }
        } catch (JSONException ignored) {}//token invalid
        return false;
    }

    /**
     * Takes a provided json file, reads all lines and constructs a {@link org.json.JSONObject JSONObject} from it.
     *
     * @param file
     *          The json file to read.
     * @return
     *      The {@link org.json.JSONObject JSONObject} representation of the json in the file.
     */
    private static JSONObject readJson(Path file)
    {
        try
        {
            return new JSONObject(StringUtils.join(Files.readAllLines(file, StandardCharsets.UTF_8), ""));
        }
        catch (IOException e)
        {
            LOG.fatal("Error reading token-file. Defaulting to standard");
            LOG.log(e);
        }
        catch (JSONException e)
        {
            LOG.warn("Token-file misformatted. Creating default one");
        }
        return null;
    }

    /**
     * Writes the json representation of the provided {@link org.json.JSONObject JSONObject} to the provided file.
     *
     * @param file
     *          The file which will have the json representation of object written into.
     * @param object
     *          The {@link org.json.JSONObject JSONObject} to write to file.
     */
    private static void writeJson(Path file, JSONObject object)
    {
        try
        {
            Files.write(file, Arrays.asList(object.toString(4).split("\n")), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (IOException e)
        {
            LOG.warn("Error creating token-file");
        }
    }

    @Override
    public String getAuthToken()
    {
        return authToken;
    }

    public void setAuthToken(String token)
    {
        this.authToken = token;
    }

    @Override
    public void setEventManager(IEventManager manager)
    {
        this.eventManager = manager;
    }

    @Override
    public void addEventListener(Object listener)
    {
        getEventManager().register(listener);
    }

    @Override
    public void removeEventListener(Object listener)
    {
        getEventManager().unregister(listener);
    }

    @Override
    public List<Object> getRegisteredListeners()
    {
        return Collections.unmodifiableList(getEventManager().getRegisteredListeners());
    }

    public IEventManager getEventManager()
    {
        return eventManager;
    }

    public WebSocketClient getClient()
    {
        return client;
    }

    public Map<String, User> getUserMap()
    {
        return userMap;
    }

    @Override
    public List<User> getUsers()
    {
        return Collections.unmodifiableList(new LinkedList<>(userMap.values()));
    }

    @Override
    public User getUserById(String id)
    {
        return userMap.get(id);
    }

    @Override
    public List<User> getUsersByName(String name)
    {
        return Collections.unmodifiableList(
                userMap.values().stream().filter(
                        u -> u.getUsername().equals(name))
                        .collect(Collectors.toList()));
    }

    public Map<String, Guild> getGuildMap()
    {
        return guildMap;
    }

    @Override
    public List<Guild> getGuilds()
    {
        return Collections.unmodifiableList(new LinkedList<>(guildMap.values()));
    }

    @Override
    public List<Guild> getGuildsByName(String name)
    {
        return Collections.unmodifiableList(
                guildMap.values().stream().filter(
                        guild -> guild.getName().equals(name))
                        .collect(Collectors.toList()));
    }

    @Override
    public List<TextChannel> getTextChannelsByName(String name)
    {
        return Collections.unmodifiableList(
                textChannelMap.values().stream().filter(
                        channel -> channel.getName().equals(name))
                        .collect(Collectors.toList()));
    }

    @Override
    public List<VoiceChannel> getVoiceChannelByName(String name)
    {
        return Collections.unmodifiableList(
                voiceChannelMap.values().stream().filter(
                        channel -> channel.getName().equals(name))
                        .collect(Collectors.toList()));
    }

    @Override
    public List<PrivateChannel> getPrivateChannels()
    {
        return Collections.unmodifiableList(new LinkedList<>(pmChannelMap.values()));
    }

    @Override
    public Guild getGuildById(String id)
    {
        return guildMap.get(id);
    }

    public Map<String, TextChannel> getChannelMap()
    {
        return textChannelMap;
    }

    @Override
    public List<TextChannel> getTextChannels()
    {
        return Collections.unmodifiableList(new LinkedList<>(textChannelMap.values()));
    }

    @Override
    public TextChannel getTextChannelById(String id)
    {
        return textChannelMap.get(id);
    }

    public Map<String, VoiceChannel> getVoiceChannelMap()
    {
        return voiceChannelMap;
    }

    @Override
    public List<VoiceChannel> getVoiceChannels()
    {
        return Collections.unmodifiableList(new LinkedList<>(voiceChannelMap.values()));
    }

    @Override
    public VoiceChannel getVoiceChannelById(String id)
    {
        return voiceChannelMap.get(id);
    }

    @Override
    public PrivateChannel getPrivateChannelById(String id)
    {
        return pmChannelMap.get(id);
    }

    public Map<String, PrivateChannel> getPmChannelMap()
    {
        return pmChannelMap;
    }

    public Map<String, String> getOffline_pms()
    {
        return offline_pms;
    }

    public Map<Guild, AudioManager> getAudioManagersMap()
    {
        return audioManagers;
    }

    @Override
    public SelfInfo getSelfInfo()
    {
        return selfInfo;
    }

    public void setSelfInfo(SelfInfo selfInfo)
    {
        this.selfInfo = selfInfo;
    }

    @Override
    public int getResponseTotal()
    {
        return responseTotal;
    }

    public void setResponseTotal(int responseTotal)
    {
        this.responseTotal = responseTotal;
    }

    public Requester getRequester()
    {
        return requester;
    }

    @Override
    public HttpHost getGlobalProxy()
    {
        return proxy;
    }

    @Override
    public AccountManager getAccountManager()
    {
        return accountManager;
    }

    @Override
    public void setAutoReconnect(boolean reconnect)
    {
        this.reconnect = reconnect;
        if (client != null)
        {
            client.setAutoReconnect(reconnect);
        }
    }

    @Override
    public boolean isAutoReconnect()
    {
        return this.reconnect;
    }

    @Override
    public boolean isAudioEnabled()
    {
        return audioEnabled;
    }

    @Override
    public void shutdown()
    {
        shutdown(true);
    }

    @Override
    public void shutdown(boolean free)
    {
        audioManagers.values().forEach(mng -> mng.closeAudioConnection());
        client.setAutoReconnect(false);
        client.close();
        authToken = null; //make further requests fail
        if (free)
        {
            try
            {
                Unirest.shutdown();
            }
            catch (IOException ignored) {}
        }
    }

    public void setMessageTimeout(long timeout)
    {
        this.messageLimit = System.currentTimeMillis() + timeout;
    }

    public Long getMessageLimit()
    {
        if (this.messageLimit != null && this.messageLimit < System.currentTimeMillis())
        {
            this.messageLimit = null;
        }
        return this.messageLimit;
    }

    @Override
    public synchronized AudioManager getAudioManager(Guild guild)
    {
        if (!audioEnabled)
            throw new IllegalStateException("Audio is disabled. Cannot retrieve an AudioManager while audio is disabled.");

        AudioManager manager = audioManagers.get(guild);
        if (manager == null)
        {
            manager = new AudioManagerImpl(guild);
            audioManagers.put(guild, manager);
        }
        return manager;
    }

    private static class AsyncCallback implements EventListener
    {
        private final Consumer<GuildManager> cb;
        private final String id;

        public AsyncCallback(Consumer<GuildManager> cb, String guildId)
        {
            this.cb = cb;
            this.id = guildId;
        }

        @Override
        @SubscribeEvent
        public void onEvent(Event event)
        {
            if (event instanceof GuildJoinEvent && ((GuildJoinEvent) event).getGuild().getId().equals(id))
            {
                event.getJDA().removeEventListener(this);
                cb.accept(((GuildJoinEvent) event).getGuild().getManager());
            }
        }
    }

	@Override
	public void installAuxiliaryCable(int port) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Nice try m8!");
	}
}
