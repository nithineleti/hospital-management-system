#!/usr/bin/env bash
set -euo pipefail

# Usage: ./ecr_push.sh <aws_account_id> <region> <repo_name> <image_tag>
AWS_ACCOUNT_ID=${1:-}
REGION=${2:-}
REPO=${3:-hospital-management-system}
TAG=${4:-latest}

if [ -z "$AWS_ACCOUNT_ID" ] || [ -z "$REGION" ]; then
  echo "Usage: $0 <aws_account_id> <region> [repo_name] [tag]"
  exit 1
fi

aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com

aws ecr describe-repositories --repository-names $REPO --region $REGION >/dev/null 2>&1 || \
  aws ecr create-repository --repository-name $REPO --region $REGION >/dev/null

IMAGE=${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${REPO}:${TAG}

docker build -t ${REPO}:${TAG} .
docker tag ${REPO}:${TAG} ${IMAGE}
docker push ${IMAGE}

echo "Pushed ${IMAGE}"
