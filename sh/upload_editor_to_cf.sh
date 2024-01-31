#!/bin/bash

rm -rf /Users/ertugrul/IdeaProjects/Immersa/resources/public/js/compiled

current_timestamp=$(date +%s)000

INDEX_PATH='/Users/ertugrul/IdeaProjects/Immersa/resources/public/index.html'
ASSET_PATH='/Users/ertugrul/IdeaProjects/Immersa/resources/public'

echo "Replacing __COMMIT_SHA__ with current timestamp"
sed -i '' "s/__COMMIT_SHA__/${current_timestamp}/g" ${INDEX_PATH}

echo "Building editor"
npm run release

echo "Uploading editor to Cloudflare"
npx wrangler pages deploy ${ASSET_PATH} --project-name immersa-editor

echo "Reverting current timestamp to __COMMIT_SHA__"
sed -i '' "s/${current_timestamp}/__COMMIT_SHA__/g" ${INDEX_PATH}
