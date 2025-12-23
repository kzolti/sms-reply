#!/usr/bin/env bash
set -euo pipefail

gradle wrapper

git clone https://gitlab.com/kada.zoli/fdroiddata.git
cd fdroiddata

printf "sdk_path: /opt/android-sdk\nserverwebroot: /tmp/fdroid\n" > config.yml
chmod 600 config.yml
mkdir -p /tmp/fdroid

echo "fdroid-env/" >> .gitignore
yay -S python311

python3.11 -m venv fdroid-env
source fdroid-env/bin/activate

pip install --upgrade pip
pip install fdroidserver

export PATH=/home/zolti/Android/Sdk/build-tools/34.0.0:$PATH

fdroid readmeta
fdroid import --url https://github.com/kzolti/sms-reply --subdir app
fdroid checkupdates hu.kadatsoft.smsreply
fdroid rewritemeta hu.kadatsoft.smsreply
fdroid lint hu.kadatsoft.smsreply
fdroid build -v -l hu.kadatsoft.smsreply


fdroid readmeta
fdroid import --url https://github.com/kzolti/sms-reply --subdir app
fdroid checkupdates hu.kadatsoft.smsreply
fdroid rewritemeta hu.kadatsoft.smsreply
fdroid lint hu.kadatsoft.smsreply
fdroid build -v -l hu.kadatsoft.smsreply