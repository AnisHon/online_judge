s3fs case /mnt/minio/case -o passwd_file=/etc/passwd-s3fs -o url="${MINIO_URL}" -o use_path_request_style
/opt/go-judge --http-addr=:5050 --silent=true --file-timeout=5m