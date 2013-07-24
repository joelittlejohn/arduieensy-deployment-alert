PID=$(cat arduieensy-deployment-alert.pid)

kill $PID
sleep 1

if ps -p $PID > /dev/null
then 
    echo sigterm failed, attempting sigkill...
    kill -KILL $PID
fi
