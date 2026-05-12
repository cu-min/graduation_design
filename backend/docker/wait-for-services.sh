#!/bin/sh
set -eu

wait_for_host_port() {
  name="$1"
  host="$2"
  port="$3"
  timeout="${4:-90}"

  echo "Waiting for ${name} at ${host}:${port}..."
  start_time="$(date +%s)"

  while ! nc -z "$host" "$port" >/dev/null 2>&1; do
    current_time="$(date +%s)"
    if [ $((current_time - start_time)) -ge "$timeout" ]; then
      echo "Timed out waiting for ${name} at ${host}:${port}"
      exit 1
    fi
    sleep 2
  done

  echo "${name} is available."
}

wait_for_host_port "MySQL" "${DB_HOST:-mysql}" "${DB_PORT:-3306}" "${WAIT_FOR_DB_TIMEOUT:-120}"
wait_for_host_port "Redis" "${REDIS_HOST:-redis}" "${REDIS_PORT:-6379}" "${WAIT_FOR_REDIS_TIMEOUT:-60}"

exec "$@"
