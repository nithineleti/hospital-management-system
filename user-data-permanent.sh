#!/bin/bash
set -e

# Update system
yum update -y
yum install -y java-17-amazon-corretto-headless maven git

# Clone and build
cd /home/ec2-user
git clone https://github.com/nithineleti/hospital-management-system.git
cd hospital-management-system

# Build the project
/opt/maven/bin/mvn clean package -DskipTests -q

# Create systemd service for the application
sudo tee /etc/systemd/system/hospital-app.service > /dev/null <<EOF
[Unit]
Description=Hospital Management System
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/home/ec2-user/hospital-management-system
ExecStart=/usr/lib/jvm/java-17-amazon-corretto/bin/java -jar /home/ec2-user/hospital-management-system/target/hospital-management-system-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Enable and start the service
sudo systemctl daemon-reload
sudo systemctl enable hospital-app.service
sudo systemctl start hospital-app.service

# Log the startup
sudo systemctl status hospital-app.service >> /tmp/hospital-app-startup.log
