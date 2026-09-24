# Glintbox — Play Console kit

Everything you need to fill in the Play Console. Copy/paste friendly.

## Store listing

**App name (30 max):** `Glintbox: Status Keeper`

> Do **not** put "WhatsApp" in the app name, icon or first line of the short description.
> Google treats that as impersonation / IP infringement and this app category gets rejected for it often.

**Short description (80 max):**
`Save, view & organise statuses in a beautiful, private, offline gallery.`

**Full description:**

```
Glintbox keeps the moments worth keeping.

View the statuses already downloaded to your phone in a gorgeous full-screen viewer, save the ones you love in one tap, and keep them organised in your own gallery — all 100% offline and private.

✦ ONE-TAP & BULK SAVE
Save a single status, multi-select several, or save everything new at once.

✦ AUTO-SAVE
Optionally copy new statuses automatically in the background, images only or videos too.

✦ YOUR FOLDER, YOUR RULES
Saves to Internal storage/WhatsApp Statuses by default, or pick any folder on your phone or SD card.

✦ BEAUTIFUL VIEWER
Pinch-to-zoom photos, smooth video playback, GIF support, swipe between statuses and see roughly how long each status has left.

✦ SHARE & REPOST
Share to any app, or repost straight back to your status.

✦ SIX HANDCRAFTED THEMES
Aurora, Ember, Lagoon, Orchid, Jade and Midas palettes, light & dark modes, pure-black AMOLED mode and Material You colours.

✦ PRIVATE BY DESIGN
No internet permission. No ads. No tracking. No accounts. No "all files" access — you grant access to exactly one folder through Android's own picker.

✦ SUPPORTS
Standard and Business editions of the popular messenger.

RESPECT OTHERS
Statuses belong to the people who posted them. Only save or re-share content with the owner's permission.

DISCLAIMER
Glintbox is an independent app. It is not affiliated with, associated with, authorised, endorsed by or in any way officially connected to WhatsApp LLC or Meta Platforms, Inc. All trademarks belong to their respective owners.
```

**Category:** Tools (or Photography) · **Tags:** Tools, Photo & video · **Contains ads:** No · **In-app purchases:** No

**Graphics:** 512×512 icon (export `app/src/main/res/drawable/ic_launcher_*` via Android Studio → *Image Asset* or any SVG tool), 1024×500 feature graphic, 4–8 phone screenshots.
Avoid showing the WhatsApp logo or real people's statuses in screenshots — use your own test content.

## App content answers

| Section            | Answer                                                                                         |
|--------------------|------------------------------------------------------------------------------------------------|
| Privacy policy URL | Host `PRIVACY_POLICY.md` (e.g. GitHub Pages / Google Sites) and paste the URL                  |
| Ads                | No ads                                                                                         |
| App access         | All functionality available without special access                                             |
| Content rating     | IARC questionnaire: utility app, no user-generated content hosted, no violence etc. → Everyone |
| Target audience    | 13+ (select 13–15, 16–17, 18+). Do **not** target children.                                    |
| News app           | No                                                                                             |
| Government app     | No                                                                                             |
| Financial features | None                                                                                           |
| Health             | No                                                                                             |

## Data safety form

- **Does your app collect or share any of the required user data types?** → **No**
  (All file processing is on-device and nothing is transmitted; Google's definition of "collect" means transmitted off the device.)
- **Is all user data encrypted in transit?** → Not applicable (no data leaves the device)
- **Do you provide a way for users to request that their data is deleted?** → Not applicable / uninstalling removes all app data

## Permissions declaration

| Permission                                                    | Why                                                        | Needs a Play declaration? |
|---------------------------------------------------------------|------------------------------------------------------------|---------------------------|
| `READ_EXTERNAL_STORAGE` (maxSdk 29)                           | Read status files on Android 8–10                          | No                        |
| `WRITE_EXTERNAL_STORAGE` (maxSdk 29)                          | Write to `WhatsApp Statuses` on Android 8–10               | No                        |
| `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `ACCESS_NETWORK_STATE` | Merged automatically by WorkManager for optional auto-save | No                        |

Not requested (by design, so no sensitive-permission declarations are needed):
`MANAGE_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `INTERNET`, `POST_NOTIFICATIONS`, any foreground-service type, `AD_ID`.

## Policy checklist

- [x] Targets API 37 (meets Play's target-API requirement)
- [x] No "All files access" — Storage Access Framework only on Android 11+
- [x] No brand name/logo of a third party in the app name or icon; disclaimer in-app and in listing
- [x] In-app privacy policy + hostable policy
- [x] Content-ownership notice shown during onboarding and in the gallery
- [x] No ads, no tracking, no network access
- [x] Adaptive + themed (monochrome) icon, splash screen API, edge-to-edge, predictive back
- [x] Release build minified with R8 and resources shrunk
- [x] Replace `support@glintbox.app` in `strings.xml` and `PRIVACY_POLICY.md` with your real inbox
- [x] Create an upload keystore and `keystore.properties` (see README)
