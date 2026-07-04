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
