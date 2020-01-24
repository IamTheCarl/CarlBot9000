Carl Bot 9000 was a quick and dirty project I did to help me learn python quickly for a class I was taking.
Turns out people like the bot, so I've made a repository for it. People may download, learn from, and even update the bot.

The bot is currently half-baked and needs a lot of love and care. Right now the major issues are in storing data in non-volatile ways.

We have an offical Discord Server for the bot:
https://discord.gg/wQrFn6g

### Running the bot

To ensure that the bot runs, you would need to create a `main_config.json` file in the main hierarchy.

The content of the json file should be this:

```
{
    "token": "The bot's discord token",
    "owners": ["Owner(s) of this bot in string"],
    "danbooru_api_key": "API KEY FOR DANBOORU"
}
```
