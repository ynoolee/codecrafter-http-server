#!/bin/bash

# 10개의 병렬 요청을 보낼 수 있는 스크립트
for i in {1..10}; do
  curl -v "http://localhost:4221" &
done

# 모든 백그라운드 작업이 완료될 때까지 대기
wait
