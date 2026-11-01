# SFS EC2 Deployment Notes

## Jar Deployment

Build locally:

```bash
mvn clean package -DskipTests
```

Copy the jar to EC2:

```bash
scp -i ~/Downloads/sfs-RSA-keyPair.pem target/sfs-0.0.1-SNAPSHOT.jar ec2-user@13.235.101.204:/home/ec2-user/
```

Deploy on EC2:

```bash
ssh -i ~/Downloads/sfs-RSA-keyPair.pem ec2-user@13.235.101.204

sudo systemctl stop sfs
sudo cp /opt/sfs-app/app.jar /opt/sfs-app/app.jar.backup.$(date +%Y%m%d_%H%M%S)
sudo mv /home/ec2-user/sfs-0.0.1-SNAPSHOT.jar /opt/sfs-app/app.jar
sudo chown ec2-user:ec2-user /opt/sfs-app/app.jar
sudo chmod 755 /opt/sfs-app/app.jar
sudo systemctl start sfs
sudo systemctl status sfs --no-pager
```

## Single-number production review OTP

Production must not activate the broad local `FakeOtpService`. To bypass
Twilio for one approved test/review number, configure the existing
single-number review path in `/etc/sfs-app.env`:

```bash
APP_REVIEW_ENABLED=true
APP_REVIEW_PHONE_NUMBER=<E.164 phone number>
APP_REVIEW_FIXED_OTP=<six-digit OTP>
APP_REVIEW_RESEND_AFTER_SECONDS=30
```

Keep `/etc/sfs-app.env` owned by `root:root` with mode `600`, then restart
`sfs`. Verify presence without printing either secret:

```bash
sudo awk -F= '
  $1=="APP_REVIEW_ENABLED" { enabled=$2 }
  $1=="APP_REVIEW_PHONE_NUMBER" { phone=length($2) }
  $1=="APP_REVIEW_FIXED_OTP" { otp=length($2) }
  END { printf "enabled=%s phone_configured=%s otp_configured=%s\n", enabled, (phone>0?"yes":"no"), (otp==6?"yes":"no") }
' /etc/sfs-app.env
```

## Instagram Reels Meta Sync Environment

Instagram Reels sync is configured through a server-only environment file. Do not commit Meta secrets to `application.yml` or any Git-tracked file.

Create or update `/etc/sfs-app.env` on EC2:

```bash
sudo vi /etc/sfs-app.env
```

Required keys:

```bash
META_GRAPH_API_VERSION=v25.0
META_APP_ID=<set-on-server>
META_APP_SECRET=<set-on-server>
META_ACCESS_TOKEN=<set-on-server>
META_FACEBOOK_PAGE_ID=984741401382194
META_INSTAGRAM_BUSINESS_ACCOUNT_ID=17841479727273049
META_INSTAGRAM_SYNC_ENABLED=false
```

Secure the file:

```bash
sudo chmod 600 /etc/sfs-app.env
sudo chown root:root /etc/sfs-app.env
```

Attach the file to the `sfs` systemd service:

```bash
sudo systemctl edit sfs
```

Add this drop-in content if it is not already present:

```ini
[Service]
EnvironmentFile=/etc/sfs-app.env
```

Reload and restart:

```bash
sudo systemctl daemon-reload
sudo systemctl restart sfs
sudo systemctl status sfs --no-pager
```

Verify without printing secrets:

```bash
sudo systemctl show sfs --property=EnvironmentFiles

sudo bash -c 'source /etc/sfs-app.env && echo $META_GRAPH_API_VERSION && echo $META_FACEBOOK_PAGE_ID && echo $META_INSTAGRAM_BUSINESS_ACCOUNT_ID && echo ${#META_ACCESS_TOKEN} && echo ${#META_APP_SECRET}'
```

Expected:

- `EnvironmentFiles=/etc/sfs-app.env`
- access token length greater than `50`
- app secret length greater than `10`

Keep `META_INSTAGRAM_SYNC_ENABLED=false` until scheduled sync is intentionally enabled. Use the dashboard manual sync action for initial testing.

## Marketplace Demo Content (first deploy with V174/V175)

The backend loads the Marketplace demo stores and Allied Services workers by itself on the
first start after this deploy (`MarketplaceDemoSeedRunner`); no dashboard step is needed.
Before copying the jar, add the contact and worker-login numbers to `/etc/sfs-app.env`
(see `docs/PRODUCTION_ENV_TEMPLATE.md`):

```bash
sudo nano /etc/sfs-app.env
```

```bash
SFS_MARKETPLACE_DEMO_SEED_ENABLED=true
SFS_DEMO_CONTACT_PHONE=8790012345
SFS_DEMO_WORKER_LOGIN_PHONES=9XXXXXXXX1,9XXXXXXXX2,9XXXXXXXX3,9XXXXXXXX4
```

Use real numbers your company controls: the contact number receives customers' calls and
WhatsApp messages, and each worker-login number can sign in as that demo worker.

After the restart, check the log once:

```bash
sudo journalctl -u sfs --since "10 min ago" --no-pager | grep -E "now at version|Marketplace demo seed"
```

Expected: `now at version v175` and `Marketplace demo seed: loaded 13 demo stores/workers, added the contact number to 0 existing ones (contact number configured)`
(13 = 4 new stores + 5 V6 sample stores decorated + 4 workers; fewer if some V6 stores are missing
or were already edited, or a worker number was not set). Later restarts log
`Marketplace demo seed: nothing to do`.

If the numbers were missing or invalid on that first start, the stores still load without
Call/WhatsApp and the workers are skipped. Put the real numbers in `/etc/sfs-app.env` and restart
(`sudo systemctl restart sfs`): the next start adds the missing workers and their connections and
puts the contact number on the demo stores, logging e.g.
`loaded 4 demo stores/workers, added the contact number to 4 existing ones`.

### More demo stores, workers and self-hosted photos (November 2026)

The next start after this deploy also:

- repoints the demo store photos from Wikimedia/Flickr hot-links to copies bundled in the jar
  (`/api/public/demo-images/marketplace/*`), so they show on the website too. Only photos still
  at their original hot-link URL change; photos replaced in the dashboard are untouched;
- adds 4 demo stores (Urban Nest Furniture Studio, Lumina Electricals & Lighting, StoneCraft
  Tiles & Bath Gallery, AquaLine Sanitary Hub) with hours, products, services and photos;
- adds up to 10 more demo workers (painters, carpenters, electricians, plumbers and masons) -
  each only once its login number is appended to `SFS_DEMO_WORKER_LOGIN_PHONES`, in this order
  after the first four: Ramesh, Imran, Deepak, Vikas, Mohan, Sunil, Arjun, Sanjay, Rajesh, Prakash.

```bash
SFS_DEMO_WORKER_LOGIN_PHONES=<4 existing numbers>,<Ramesh>,<Imran>,<Deepak>,<Vikas>,<Mohan>,<Sunil>,<Arjun>,<Sanjay>,<Rajesh>,<Prakash>
```

Expected log: `loaded 14 demo stores/workers, ..., added 26+ demo photos` (4 stores + 10 workers,
fewer if numbers are missing; the photo count includes the repointed store photos). Numbers can be
appended later; each restart adds the workers that are still missing.
