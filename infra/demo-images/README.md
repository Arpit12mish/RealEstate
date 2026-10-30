# Spotlight demo images (from Figma)

Source: Figma file `I0nm11Qarb0dmXDhAHtG3H`, frame `Spotlight_Image_Bank` (node `3499:2887`), exported
2026-10-27. 4 card photos per Festive-picks tab, 1024x1024 JPEG:

| Files | Tab / use |
|---|---|
| `shops-1..4.jpg` | Local Shops - demo store cover photos |
| `home-services-1..4.jpg` | Home Services - demo worker gallery photos |
| `builders-*`, `properties-*`, `architects-*`, `interior-designers-*`, `calculators-*`, `brands-*` | the other tabs (builder/property cards use real project covers) |

These are NOT referenced by the app or the demo seed yet: production media is served from the S3 /
CloudFront bucket, so the photos must be uploaded there first and the seed (`marketplace/demo-seed.sql`)
pointed at the resulting URLs. The Figma export URLs expire after 7 days; these files are the durable copy.
