#!/usr/bin/env bash
set -euo pipefail

# Mindig a script könyvtárába lép
cd "$(dirname "$0")"

run() {
    echo -e "\n+ $*"   # parancs kiírása egy új sorban
    "$@"               # parancs futtatása a stdout/stderr-rel
}

# Gradle wrapper létrehozása (ha gradle elérhető)
run gradle wrapper || echo "Gradle nem található, ugorjuk."

# Klónozzuk a fdroiddata repót
run git clone https://gitlab.com/kada.zoli/fdroiddata.git
run cd fdroiddata

# config.yml létrehozása
run printf "sdk_path: /opt/android-sdk\nserverwebroot: /tmp/fdroid\n" > config.yml
run chmod 600 config.yml
run mkdir -p /tmp/fdroid

# fdroid-env hozzáadása .gitignore-hoz
grep -qxF "fdroid-env/" .gitignore || run echo "fdroid-env/" >> .gitignore
grep -qxF "config.yml/" .gitignore || run echo "config.yml" >> .gitignore

# Python 3.11 telepítése
run yay -S --needed --noconfirm python311

# virtuális környezet létrehozása
run python3.11 -m venv fdroid-env
# shell option miatt source nem run függvénnyel
echo "+ source fdroid-env/bin/activate"
source fdroid-env/bin/activate

# pip frissítése és fdroidserver telepítése
run pip install --upgrade pip
run pip install fdroidserver

# Android build-tools PATH
echo "+ export PATH=/home/zolti/Android/Sdk/build-tools/34.0.0:\$PATH"
export PATH=/home/zolti/Android/Sdk/build-tools/34.0.0:$PATH

# F-Droid parancsok a saját appodra
# run git add .
# run git commit -m "Add fdroid config and env to .gitignore"
run fdroid import --url https://github.com/kzolti/sms-reply --subdir app
run fdroid checkupdates hu.kadatsoft.smsreply
run fdroid rewritemeta hu.kadatsoft.smsreply
run fdroid lint hu.kadatsoft.smsreply
run fdroid build -v -l hu.kadatsoft.smsreply
