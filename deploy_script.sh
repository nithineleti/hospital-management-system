#!/bin/bash
PROJECT_DIR="/home/ubuntu/hospital-management-system"
if [ ! -d "$PROJECT_DIR" ]; then
    git clone https://github.com/nithineleti/hospital-management-system.git "$PROJECT_DIR"
fi
cd "$PROJECT_DIR"
git pull origin main
chmod +x mvnw
./mvnw clean package -DskipTests
sudo docker stop hospital-app || true
sudo docker rm hospital-app || true
sudo docker build -t hospital-app .
# Map host port 80 to container port 8080 for clean URL
sudo docker run -d --name hospital-app --restart always -p 80:8080 hospital-app
