#!/bin/bash

#Only this user may control the Minecraft servers.
TARGET_USER="discord-bots"

if [ "$TARGET_USER" != "$USER" ]; then
    echo "Only the user $TARGET_USER may run this script."
    echo "Enter your password to log in as $TARGET_USER."
    sudo runuser $TARGET_USER -c "$0 $@"
    exit
fi

SESSION="discord-bot-`basename $PWD`"

#COMMAND="bash"
#COMMAND="--audit"
COMMAND="python3 carlbot.py"
SANDBOX="firejail --profile=/home/shared/sandbox-profiles/discord-bot.profile --whitelist=$PWD $COMMAND"

start() {
    echo "Session name: $SESSION"
    echo "Launching from secured sandbox."
    tmux new-session -d -s $SESSION $SANDBOX
    tmux detach -s $SESSION
}

stop() {
    echo "Stopping session: $SESSION"
    tmux send-keys -t $SESSION 'shutdown' C-m
}

attach() {
    echo "Attaching to session: $SESSION"
    tmux attach-session -t $SESSION
}

case "$1" in
start)
    start
;;
stop)
    stop
;;
attach)
    attach
;;
restart)
    stop
    sleep 1
    echo "Restarting bot..."
    sleep 1
    start
;;
*)
    echo "Usage: %0 (start|stop|restart|attach)"
;;
esac

