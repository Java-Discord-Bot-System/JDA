package com.almightyalpaca.discord.bot.system.util.emoji;

import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;

public class EmojiUtil
{
    private static final Map<String, String> toUTF8;
    private static final Map<String, String> toDiscord;
    private static final Map<String, String> shortcuts;

    static
    {
        final Map<String, String> tempToUTF8 = new HashMap<>();
        final Map<String, String> tempToDiscord = new HashMap<>();

        try
        {
            final JSONArray array = new JSONArray(new InputStreamReader(EmojiUtil.class.getResourceAsStream("emojis.json"), "UTF-8"));

            for (int i = 0; i < array.length(); i++)
            {
                final JSONObject object = array.getJSONObject(i);

                final String name = ":" + object.getString("emoji") + ":";
                final JSONArray utfArray = object.getJSONArray("surrogates");

                tempToUTF8.put(name, utfArray.getString(0));

                for (int j = 0; j < utfArray.length(); j++)
                {
                    final String emoji = utfArray.getString(j);
                    tempToDiscord.put(emoji, name);
                }
            }

        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }

        toUTF8 = Collections.unmodifiableMap(tempToUTF8);
        toDiscord = Collections.unmodifiableMap(tempToDiscord);

        final Map<String, String> tempShortcuts = new HashMap<>();

        try
        {
            final JSONArray array = new JSONArray(new InputStreamReader(EmojiUtil.class.getResourceAsStream("shortcuts.json"), "UTF-8"));

            for (int i = 0; i < array.length(); i++)
            {
                final JSONObject object = array.getJSONObject(i);

                final String name = ":" + object.getString("emoji") + ":";
                final JSONArray shortcutsArray = object.getJSONArray("shortcuts");

                for (int j = 0; j < shortcutsArray.length(); j++)
                {
                    final String emoji = shortcutsArray.getString(j);
                    tempShortcuts.put(emoji, name);
                }

            }

        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }

        shortcuts = Collections.unmodifiableMap(tempShortcuts);
    }

    public static String escapeShortcuts(String string)
    {
        for (final Entry<String, String> entry : EmojiUtil.shortcuts.entrySet())
        {
            string = string.replace(entry.getKey(), entry.getValue());

        }
        return string;
    }

    public static String toDiscord(String string)
    {
        for (final Entry<String, String> entry : EmojiUtil.toDiscord.entrySet())
        {
            string = string.replace(entry.getValue(), entry.getKey());
        }
        return string;
    }

    public static String toUTF8(String string)
    {
        for (final Entry<String, String> entry : EmojiUtil.toUTF8.entrySet())
        {
            string = string.replace(entry.getKey(), entry.getValue());
        }
        return string;
    }
}
