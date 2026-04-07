#!/bin/bash
yum update -y
yum install -y docker git
service docker start
usermod -a -G docker ec2-user
cd /home/ec2-user
git clone https://github.com/nithineleti/hospital-management-system.git
cd hospital-management-system
docker build -t hospital-app .
docker run -d -p 8080:8080 --name hospital-container hospital-app
