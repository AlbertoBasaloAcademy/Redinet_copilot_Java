#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

BASE_URL="${BASE_URL:-http://localhost:8080}"
JAR_PATH="target/astrobookings-1.0-SNAPSHOT.jar"

_tmp_dir="$(mktemp -d)"
cleanup() {
  if [[ -n "${SERVER_PID:-}" ]]; then
    kill "$SERVER_PID" >/dev/null 2>&1 || true
    wait "$SERVER_PID" >/dev/null 2>&1 || true
  fi
  rm -rf "$_tmp_dir" >/dev/null 2>&1 || true
}
trap cleanup EXIT

http_request() {
  local method="$1"
  local url="$2"
  local body_file="${3:-}"
  local out_file="$4"

  if [[ -n "$body_file" ]]; then
    curl -sS -o "$out_file" -w "%{http_code}" \
      -H "Content-Type: application/json" \
      -X "$method" \
      --data-binary "@$body_file" \
      "$url"
  else
    curl -sS -o "$out_file" -w "%{http_code}" \
      -X "$method" \
      "$url"
  fi
}

assert_status() {
  local actual="$1"
  local expected="$2"
  local label="$3"

  if [[ "$actual" != "$expected" ]]; then
    echo "FAIL: $label (expected $expected, got $actual)" >&2
    return 1
  fi
}

step() {
  echo "==> $1"
}

ok() {
  echo "OK: $1"
}

assert_body_contains() {
  local file="$1"
  local needle="$2"
  local label="$3"

  if ! grep -Fq "$needle" "$file"; then
    echo "FAIL: $label (missing '$needle')" >&2
    echo "Response body:" >&2
    cat "$file" >&2
    return 1
  fi
}

extract_json_string_field() {
  local file="$1"
  local field="$2"
  sed -n "s/.*\"$field\"[[:space:]]*:[[:space:]]*\"\([^\"]*\)\".*/\1/p" "$file" | head -n 1
}

start_server() {
  if [[ ! -f "$JAR_PATH" ]]; then
    ./mvnw -q -DskipTests package
  fi

  java -jar "$JAR_PATH" >"$_tmp_dir/server.log" 2>&1 &
  SERVER_PID=$!

  local attempts=40
  local health_out="$_tmp_dir/health.json"
  for _ in $(seq 1 "$attempts"); do
    if curl -sS -o "$health_out" -w "%{http_code}" "$BASE_URL/rockets" | grep -q "^200$"; then
      return 0
    fi
    sleep 0.25
  done

  echo "FAIL: server did not start at $BASE_URL" >&2
  echo "Server log:" >&2
  cat "$_tmp_dir/server.log" >&2 || true
  return 1
}

start_server

# 1) POST /rockets (valid) -> 201 + id
step "1) POST /rockets valid returns 201 + id"
req1="$_tmp_dir/create1.json"
out1="$_tmp_dir/create1.out.json"
cat >"$req1" <<'JSON'
{ "name": "Falcon", "capacity": 10, "range": "LEO", "speed": 7.8 }
JSON
code="$(http_request POST "$BASE_URL/rockets" "$req1" "$out1")"
assert_status "$code" 201 "POST /rockets valid"
assert_body_contains "$out1" '"id"' "POST /rockets valid contains id"
assert_body_contains "$out1" '"name"' "POST /rockets valid contains name"
rocket_id="$(extract_json_string_field "$out1" "id")"
if [[ -z "$rocket_id" ]]; then
  echo "FAIL: could not extract id from create response" >&2
  cat "$out1" >&2
  exit 1
fi
ok "1) Created rocket id=$rocket_id"

# 2) POST /rockets missing/blank name -> 400
step "2) POST /rockets missing name returns 400"
req_bad_name="$_tmp_dir/create-bad-name.json"
out_bad_name="$_tmp_dir/create-bad-name.out.json"
cat >"$req_bad_name" <<'JSON'
{ "capacity": 5 }
JSON
code="$(http_request POST "$BASE_URL/rockets" "$req_bad_name" "$out_bad_name")"
assert_status "$code" 400 "POST /rockets missing name"
assert_body_contains "$out_bad_name" '"error"' "POST /rockets missing name returns error JSON"
ok "2) Validation for missing name"

# 3) POST /rockets capacity out of bounds -> 400
step "3) POST /rockets invalid capacity returns 400"
req_bad_capacity="$_tmp_dir/create-bad-capacity.json"
out_bad_capacity="$_tmp_dir/create-bad-capacity.out.json"
cat >"$req_bad_capacity" <<'JSON'
{ "name": "TooBig", "capacity": 11 }
JSON
code="$(http_request POST "$BASE_URL/rockets" "$req_bad_capacity" "$out_bad_capacity")"
assert_status "$code" 400 "POST /rockets invalid capacity"
assert_body_contains "$out_bad_capacity" '"error"' "POST /rockets invalid capacity returns error JSON"
ok "3) Validation for capacity bounds"

# 4) GET /rockets -> 200 + array
step "4) GET /rockets returns 200 + array"
out_list="$_tmp_dir/list.out.json"
code="$(http_request GET "$BASE_URL/rockets" "" "$out_list")"
assert_status "$code" 200 "GET /rockets"
assert_body_contains "$out_list" '[' "GET /rockets returns array"
assert_body_contains "$out_list" 'Falcon' "GET /rockets contains created rocket"
ok "4) List rockets"

# Create a second rocket to validate name filter
step "4b) Create second rocket for filter test"
req2="$_tmp_dir/create2.json"
out2="$_tmp_dir/create2.out.json"
cat >"$req2" <<'JSON'
{ "name": "Saturn", "capacity": 3, "range": "MOON" }
JSON
code="$(http_request POST "$BASE_URL/rockets" "$req2" "$out2")"
assert_status "$code" 201 "POST /rockets second rocket"
ok "4b) Second rocket created"

# 5) GET /rockets?name=<value> -> 200 only matching
step "5) GET /rockets?name filter returns only matching"
out_filter="$_tmp_dir/filter.out.json"
code="$(http_request GET "$BASE_URL/rockets?name=Fal" "" "$out_filter")"
assert_status "$code" 200 "GET /rockets?name filter"
assert_body_contains "$out_filter" 'Falcon' "GET /rockets?name includes matching"
if grep -q 'Saturn' "$out_filter"; then
  echo "FAIL: GET /rockets?name returned non-matching rocket" >&2
  cat "$out_filter" >&2
  exit 1
fi
ok "5) Name filter"

# 6) Unsupported method to /rockets -> 405
step "6) Unsupported method returns 405"
out_405="$_tmp_dir/405.out.json"
code="$(http_request DELETE "$BASE_URL/rockets" "" "$out_405")"
assert_status "$code" 405 "DELETE /rockets not allowed"
assert_body_contains "$out_405" 'Method not allowed' "405 error payload"
ok "6) Method not allowed"

# 7) GET /rockets/{id} existing -> 200
step "7) GET /rockets/{id} existing returns 200"
out_get="$_tmp_dir/get.out.json"
code="$(http_request GET "$BASE_URL/rockets/$rocket_id" "" "$out_get")"
assert_status "$code" 200 "GET /rockets/{id} existing"
assert_body_contains "$out_get" "$rocket_id" "GET /rockets/{id} returns same id"
ok "7) Get by id existing"

# 8) GET /rockets/{id} non-existent -> 404
step "8) GET /rockets/{id} missing returns 404"
out_404="$_tmp_dir/404.out.json"
code="$(http_request GET "$BASE_URL/rockets/does-not-exist" "" "$out_404")"
assert_status "$code" 404 "GET /rockets/{id} missing"
assert_body_contains "$out_404" 'NOT_FOUND' "404 error payload"
ok "8) Get by id missing"

# 9) GET /rockets/{id} blank -> 400 (use encoded space)
step "9) GET /rockets/{id} blank returns 400"
out_blank="$_tmp_dir/blank-id.out.json"
code="$(http_request GET "$BASE_URL/rockets/%20" "" "$out_blank")"
assert_status "$code" 400 "GET /rockets/{id} blank"
assert_body_contains "$out_blank" 'INVALID_ID' "400 invalid id payload"
ok "9) Blank id"

echo "OK: 001-rocket-management e2e passed"
