# Push Notifications
Send notifications to your phone or other devices, currently supports [Pushbullet](https://www.pushbullet.com/) and [Pushover](https://pushover.net/).
## Pushbullet
You need to provide a pushbullet api key to use this service.
Go [here](https://www.pushbullet.com/#settings) and click create new token or use your existing one.
![create-new-pushbullet](imgs/create-new-pushbullet.png)

The generated key will be in the format `o.Dorf43jdDIepfKeroPewfjeIUHJ4MrOP`.

Open your client and paste the key into the `Pushbullet` config field of `Push Notifications` to enable phone notifications.
## Pushover
You need to provide both a pushover user key and api token to use this service.

Go [here](https://pushover.net/), login and your user key will be in the top right of the page.

To get an api key, click `Create an Application/API Token`, enter a name for the application such as `Runelite` and click `Create Application`.

![create-new-pushover](imgs/create-new-pushover.png)

Your api key will then be displayed at the top of the page.

Copy and paste both the user key and api token into there respective fields under the `Pushover` section to enable notifications.