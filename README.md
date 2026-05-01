# DonationAlerts
DonationAlerts is a 1.21.4 Paper plugin which allows streamers to connect DA platform to their minecraft player.
So, according to the server config, the event would be triggered when a certain amount is donated.

The project currently is not maintained. But, feel free to open a PR.

## Story
This plugin was originally created for CIS-community twitch streamer: [drakeoffc](https://twitch.tv/drakeoffc).\
But, due to the events being too boring, only one stream was hosted. Here's the VOD: https://www.youtube.com/watch?v=JTiISyA8DeM (it's in russian)\
By the way, as a dev, i didn't decide what events would end up in the plugin - that was done by another person, through whom we worked with this streamer.

Besides that, the payment for this plugin were delayed for over a week, despite the promise of payment on the day of the stream (contact me for proof).\
Unwilling to wait any longer, I wrote a short post and sent it via donation to another streamer, asking him to look into the matter.
The other streamer, not believing even the first sentence (which stated that our payment was being delayed), called the streamer directly, and the streamer managed to justify it by claiming he'd simply "forgotten," but the streamer simply ignored us.

The second excuse was even sillier: when development first started, there was no deadline.
About five days into development, I was asked, "Will it be ready in two days?" Besides the fact that I'd just started development, I was sick at the time, to which I answered a clear no.
The answer was simple: "Keep working, no problem."\
Later, this situation was described as us having fucked up the deadline.

### Conclusion
Always clarify work deadlines and payment date as well.
Also, don't rely on the "popularity" of the person you work with.

## Changes

So, let's move onto the plugin itself! There are several key changes from the original plugin shown in VOD:
- plugin was translated to English
- chances on "casino" event was simplified. there's now 40% chances to get a combination, and a 5% (out of 40%) chance to get a combination of three 6's.
- commit history was collapsed to prevent any data leaks.

## Setup

The plugin is a little bit hard to install, but i'll describe the main steps:
- Install Paper server on version 1.21.4.
- Install [CommandAPI](https://github.com/CommandAPI/CommandAPI), it's the only dependency for the plugin.
- Open two additional TCP ports: one for resourcepack hosting, second for DonationAlerts app.
- Change the ports in config to the ones you've just opened. Also, change public_ip and redirect_uri fields to the match your server IP and the desired port.
- Open https://www.donationalerts.com/application/clients and create a new app. Choose any name, but, in field "Redirect URL" set the value from the config.
- Copy App ID and API Key and set them to the client_id and client_secret values respectively.
- Start your server and execute `/da connect` as a player. You would be prompted to connect your DA account.

## Known problems

Plugin has many problems, here are the most annoying:
- resource pack is stored as .zip file which is hard to modify.
- several custom events (black hole, boss entity or casino) would break after server restart.
- profanity filter is works only with Russian language

## Credits
- [just_lofe](https://github.com/justlofe) - developer
- [Jacob](https://t.me/oveo0) - artist, designer
- [DartCat25](https://github.com/DartCat25) - shader developer for black hole, wobble and drug effects