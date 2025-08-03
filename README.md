# Push Notifications
Forward notifications to your phone or other devices, currently supports [Pushbullet](https://www.pushbullet.com/), [Pushover](https://pushover.net/) and [Gotify](https://gotify.net).

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

## Gotify
[Gotify](https://gotify.net/) is a self-hosted push notification service.

You need to provide a Gotify server URL, application token and priority.

Gotify server URL format: `http://10.0.0.30:8080/message`

Gotify token can be created under `Apps/Create Application`

![create-new-gotify](imgs/create-new-gotify.png)

Example notification

![example-gotify](imgs/gotify-example.png)

## Pushcut
[Pushcut](https://www.pushcut.io/) is a notification service on iOS, that offers the ability to add _triggers_ to notifications. Meaning, you can trigger an iOS shortcut or open a URL (including deeplinks) by tapping or acknowledging the notification on your device. 

You need to provide the following:
* **Webhook Secret**: This is automatically created when creating an account. Locate your secret within the Pushcut app by navigating to _account_ -> _webhook_ -> _secret_
* **Notification Name**: The name for a notification that you create within the app

_**NOTE:**_ Both **Webhook Secret**, and **Notification Name** are CASE SENSITIVE.

# Triggering a push notification
The easiest way to trigger a push notification is to use the standard notification api: `notifier.notify("notification text")`, the notification text will be forwarded to the configured services.

There is also the option to send a `PluginMessage` with namespace `push-notifications` and name `notify`. This will forward the value of `$.message`.

## Watchdog
Within watchdog, configure a notification of type "Plugin Message" with the namespace `push-notifications` and method `notify`.

![example-create-watchdog](imgs/create-new-watchdog.png)

Add the message data as a JSON message:
```json
{"message": "test from watchdog"}
```

![example-create-watchdog-1](imgs/create-new-watchdog-1.png)

Test the alert from within watchdog using the potion icon.

![example-pushover-watchdog](imgs/pushover-watchdog-example.png)
