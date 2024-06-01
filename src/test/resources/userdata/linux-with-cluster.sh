Content-Type: multipart/mixed; boundary="//"
MIME-Version: 1.0

--//
Content-Type: text/cloud-config; charset="us-ascii"
MIME-Version: 1.0
Content-Transfer-Encoding: 7bit
Content-Disposition: attachment; filename="cloud-config"

#cloud-config
package_update: true
packages:
  - lvm2
  - nfs-utils
  - nfs-common
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
echo 'Creating the /etc/ecs/ecs.config files.'
cat <<EOT >> /etc/ecs/ecs.config
ECS_CLUSTER=example-cluster
EOT
echo 'File /etc/ecs/ecs.config successfully created.'

log "Finished executing GoCD's user data script, now executing custom user data script from use, if present."

log "Finished executing user specified user data script."

--//
Content-Type: text/x-shellscript; charset="us-ascii"
MIME-Version: 1.0
Content-Transfer-Encoding: 7bit
Content-Disposition: attachment; filename="initialize_instance_store"

#!/bin/bash
exec > >(tee /var/log/initialize_instance_store.log | logger -t user-data -s 2>/dev/console) 2>&1
function log() {
    echo "[$(date "+%Y-%m-%d %H:%M:%S")] - $1" >> /var/log/initialize_instance_store.log
}

function try() {
   $@
   return 0
}

log "Starting to setup instance store for the docker."
INSTANCE_STORES=$(ls /dev/disk/by-id/*EC2_NVMe_Instance_Storage*-ns-1)

if [ -z "${INSTANCE_STORES}" ]; then
    log "No instance store detected."
fi

VOLUMES="$INSTANCE_STORES"
if [ -e "/dev/xvdcz" ]; then
    log "Instance has /dev/xvdcz EBS volume. Using it for docker logical volume group."
    VOLUMES="$VOLUMES /dev/xvdcz"
fi

if [ -z "${VOLUMES}" ]; then
    log "No addition volumes. Using box standard docker setup."
else
    log "Available instance stores: ${VOLUMES}."
    log "Setting up the docker logical volume group."

    service docker stop
    rm -rf /var/lib/docker/*
    dmsetup remove_all

    VOLUME_GROUP=docker
    LOGICAL_VOLUME=docker-pool

    try vgremove -y "${VOLUME_GROUP}"
    try lvremove -y "${LOGICAL_VOLUME}"

    vgcreate -y "${VOLUME_GROUP}" ${VOLUMES}
    sleep 2
    lvcreate -y -l 5%VG -n ${LOGICAL_VOLUME}\meta ${VOLUME_GROUP}
    lvcreate -y -l 90%VG -n ${LOGICAL_VOLUME} ${VOLUME_GROUP}
    sleep 2
    lvconvert -y --zero n --thinpool ${VOLUME_GROUP}/${LOGICAL_VOLUME} --poolmetadata ${VOLUME_GROUP}/${LOGICAL_VOLUME}\meta
    echo 'DOCKER_STORAGE_OPTIONS=" --storage-driver devicemapper --storage-opt dm.thinpooldev=/dev/mapper/docker-docker--pool --storage-opt dm.use_deferred_removal=true --storage-opt dm.use_deferred_deletion=true --storage-opt dm.fs=ext4 --storage-opt dm.use_deferred_deletion=true"' > /etc/sysconfig/docker-storage
    test -f /bin/systemctl && systemctl reset-failed docker.service
    service docker restart
    test -f /bin/systemctl && systemctl enable --no-block --now ecs
fi

log "Setup completed."
--//