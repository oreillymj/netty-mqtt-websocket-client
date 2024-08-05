 #!/bin/bash
dtstr=$(date +"%Y%m%d")
hashstr=$(head /dev/urandom | sha256sum | head -c10)
datehashstr="${dtstr}_${hashstr}"
echo "${datehashstr}"


docker build --no-cache -f ./Docker/Dockerfile -t localhost/mosquitto-websockets:${datehashstr} .

#docker save localhost/ensp-aws-management:${datehashstr}  | gzip > /tmp/ensp-aws-management:${datehashstr}.tar.gz


echo "docker run -it --name mosquitto-websockets -p 9001:9001/tcp --rm localhost/mosquitto-websockets:${datehashstr}"

docker run -it --name mosquitto-websockets  -p 9001:9001/tcp  --rm localhost/mosquitto-websockets:${datehashstr}
