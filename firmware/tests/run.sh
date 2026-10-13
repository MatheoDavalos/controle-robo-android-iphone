#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
build_dir=$(mktemp -d)
trap 'rm -rf "$build_dir"' EXIT
flags=(-std=c++11 -Wall -Wextra -Werror -fsanitize=undefined)
g++ "${flags[@]}" motion_test.cpp -o "$build_dir/motion_test"
"$build_dir/motion_test"
g++ "${flags[@]}" control_arbiter_test.cpp -o "$build_dir/control_arbiter_test"
"$build_dir/control_arbiter_test"
g++ "${flags[@]}" -Istubs head_test.cpp ../HeadController/Head.cpp -o "$build_dir/head_test"
"$build_dir/head_test"
python3 speed_controller_test.py
