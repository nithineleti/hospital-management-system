#!/bin/bash
sudo apt-get update -y
sudo apt-get install -y openjdk-17-jdk docker.io git
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ubuntu
sudo chmod 666 /var/run/docker.sock
