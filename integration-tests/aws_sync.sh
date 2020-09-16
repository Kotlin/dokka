#!/bin/bash

commits_to_store=3

if [ $# -lt 3 ]; then
  echo "Too little arguments. Usage: ./aws_generate <s3_address> <project_name> <path_to_project_output_dir>"
  exit 1
fi

aws_s3_address=${1%/} # Remove trailing slash if given
project_name=$2
project_path=$3

current_branch=$(git branch --show-current)
s3_project_address="${aws_s3_address}/${current_branch}/${project_name}/"
last_commits=$(git log --pretty=format:%h -n $commits_to_store)

# List all project versions
dir=$(aws s3 ls "$s3_project_address" | awk '{print $2}')

# Remove old versions
for d in $dir; do
  for commit in "${last_commits[@]}"; do
    [[ $d == "$commit/" ]] && continue
    aws s3 rm --recursive "$s3_project_address$d"
  done
done

# Sync the new one
commit_hash=$(git log -1 --format="%h")

aws s3 sync "$project_path" "$s3_project_address$commit_hash/"

echo "$commit_hash"
