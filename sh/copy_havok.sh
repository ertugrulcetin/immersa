#!/bin/bash

# Define source, destination directories, and filenames
src="node_modules/@babylonjs/havok/lib/umd/HavokPhysics.wasm"
src_js="node_modules/@babylonjs/havok/lib/umd/HavokPhysics_umd.js"
dst="resources/public/js/compiled/"
dst_vendor="src/immersa/vendor"
dst_vendor_js_old="$dst_vendor/HavokPhysics_umd.js"
dst_vendor_js_new="$dst_vendor/havok.js"

# Ensure destination directories exist
mkdir -p $dst $dst_vendor

# Copy and rename files as necessary
cp $src $dst
cp $src $src_js $dst_vendor
mv $dst_vendor_js_old $dst_vendor_js_new

echo "HavokPhysics.wasm and related files have been successfully processed."
