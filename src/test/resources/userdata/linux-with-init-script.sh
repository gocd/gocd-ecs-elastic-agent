Content-Type: multipart/mixed; boundary="//"
MIME-Version: 1.0

--//
Content-Type: text/cloud-config; charset="us-ascii"
MIME-Version: 1.0
Content-Transfer-Encoding: 7bit
Content-Disposition: attachment; filename="cloud-config"

#cloud-config
cloud_final_modules:
  - [scripts-user, always]

--//
Content-Type: text/x-shellscript; charset="us-ascii"
MIME-Version: 1.0
Content-Transfer-Encoding: 7bit
Content-Disposition: attachment; filename="userdata"

#!/bin/bash
exec > >(tee /var/log/userdata.log | logger -t user-data -s 2>/dev/console) 2>&1
function log() {
    echo "[$(date "+%Y-%m-%d %H:%M:%S")] - $1" >> /var/log/userdata.log
}
log "Starting to execute user data script."


log "Finished executing GoCD's user data script, now executing custom user data script from use, if present."
echo "this is an example init script."
log "Finished executing user specified user data script."

--//
