# Kill running ROQ processes
kill -9 `ps -ef | grep roq-management | awk '{print $2}'`
kill -9 `ps -ef | grep roq-demonstration | awk '{print $2}'`
kill -9 `ps -ef | grep roq-simulation | awk '{print $2}'`

# Kill ZK container
sudo docker stop ROQZK
sudo docker rm ROQZK