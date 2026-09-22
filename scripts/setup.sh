#!/bin/bash
# Development environment setup for Paperstow.
# Works on Ubuntu (Linux) and macOS.
# Re-runs skip any SDK package or zip that is already on disk.

if [ -z "$BASH_VERSION" ]; then
    echo -e "\033[0;31mERROR: This script must be run with bash!\033[0m"
    echo ""
    echo "Please run: bash scripts/setup.sh"
    echo "Or: chmod +x scripts/setup.sh && ./scripts/setup.sh"
    exit 1
fi

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

ANDROID_API="36"
BUILD_TOOLS_VER="35.0.0"
AVD_NAME="Paperstow_API36"
AVD_DEVICE="pixel_7"
CMDLINE_TOOLS_REV="11076708"
CACHE_DIR="${SETUP_CACHE_DIR:-$HOME/.kiro/cache}"

OS_NAME=""
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS_NAME="Linux"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    OS_NAME="macOS"
else
    echo -e "${RED}Unsupported OS: $OSTYPE${NC}"
    exit 1
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Paperstow Setup${NC}"
echo -e "${BLUE}  OS: $OS_NAME${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

command_exists() {
    command -v "$1" &> /dev/null
}

has_homebrew() {
    command_exists brew
}

has_curl() {
    command_exists curl
}

has_wget() {
    command_exists wget
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# SETUP_YES=1 skips prompts (always the default). Non-tty stdin uses the default.
ask_yes_no() {
    local prompt="$1"
    local default="${2:-y}"

    if [ "${SETUP_YES:-}" = "1" ]; then
        [ "$default" = "y" ]
        return $?
    fi
    if [ ! -t 0 ]; then
        [ "$default" = "y" ]
        return $?
    fi

    while true; do
        if [ "$default" == "y" ]; then
            echo -n -e "${BLUE}$prompt [Y/n]: ${NC}"
        else
            echo -n -e "${BLUE}$prompt [y/N]: ${NC}"
        fi

        read -r answer
        answer="${answer:-$default}"

        case "$answer" in
            [Yy]*) return 0 ;;
            [Nn]*) return 1 ;;
            *) echo -e "${RED}Please answer yes (y) or no (n)${NC}" ;;
        esac
    done
}

wait_for_manual_install() {
    local tool_name="$1"
    local install_url="$2"

    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  Manual Installation Required${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo -e "${YELLOW}The automated download failed.${NC}"
    echo ""
    echo -e "Please download and install ${tool_name} manually:"
    echo ""
    echo -e "  ${BLUE}Download URL:${NC} $install_url"
    echo ""
    echo -e "${YELLOW}Once installed, press Enter to continue...${NC}"
    read -r

    if command_exists "$tool_name"; then
        print_success "${tool_name} is now available"
        return 0
    fi
    print_warning "${tool_name} not found in PATH"
    return 1
}

is_cached() {
    local file_path="$1"
    local min_size="$2"

    if [ -f "$file_path" ] && [ -s "$file_path" ]; then
        local file_size
        file_size=$(stat -c%s "$file_path" 2>/dev/null || stat -f%z "$file_path" 2>/dev/null)
        if [ "$file_size" -ge "$min_size" ]; then
            return 0
        fi
    fi
    return 1
}

get_cached_path() {
    local url="$1"
    local basename
    basename=$(basename "$url")
    echo "$CACHE_DIR/$basename"
}

# Download once into $CACHE_DIR; later runs reuse the zip.
download_cached() {
    local url="$1"
    local min_size="${2:-1000000}"
    local dest
    dest=$(get_cached_path "$url")
    mkdir -p "$(dirname "$dest")"

    if is_cached "$dest" "$min_size"; then
        print_success "Using cached $(basename "$dest")"
        printf '%s\n' "$dest"
        return 0
    fi

    print_info "Downloading $url"
    local tmp="$dest.partial.$$"
    if has_curl; then
        curl -L "$url" -o "$tmp" --fail
    elif has_wget; then
        wget "$url" -O "$tmp" --show-progress
    else
        print_warning "curl or wget required to download $url"
        return 1
    fi
    mv "$tmp" "$dest"
    printf '%s\n' "$dest"
}

sdk_sysimage_pkg() {
    local m
    m=$(uname -m)
    if [ "$m" = "arm64" ] || [ "$m" = "aarch64" ]; then
        echo "system-images;android-${ANDROID_API};google_apis_playstore;arm64-v8a"
    else
        echo "system-images;android-${ANDROID_API};google_apis_playstore;x86_64"
    fi
}

sdk_pkg_present() {
    local pkg="$1"
    local root="${ANDROID_HOME:-$ANDROID_SDK_PATH}"
    case "$pkg" in
        platform-tools)
            [ -x "$root/platform-tools/adb" ]
            ;;
        emulator)
            [ -x "$root/emulator/emulator" ]
            ;;
        platforms\;android-*)
            [ -d "$root/platforms/${pkg#platforms;}" ]
            ;;
        build-tools\;*)
            [ -d "$root/build-tools/${pkg#build-tools;}" ]
            ;;
        system-images\;*)
            local rest="${pkg#system-images;}"
            local api="${rest%%;*}"
            local rest2="${rest#*;}"
            local tag="${rest2%%;*}"
            local abi="${rest2#*;}"
            [ -f "$root/system-images/$api/$tag/$abi/package.xml" ]
            ;;
        *)
            return 1
            ;;
    esac
}

# avdmanager treats cmdline-tools/bin as if the SDK root were the parent
# of cmdline-tools. Pin tools under cmdline-tools/latest so --sdk_root works.
ensure_cmdline_tools_layout() {
    local root="${ANDROID_HOME:-$ANDROID_SDK_PATH}"
    local latest="$root/cmdline-tools/latest"
    if [ -x "$latest/bin/sdkmanager" ]; then
        return 0
    fi
    if [ -x "$root/cmdline-tools/bin/sdkmanager" ]; then
        print_info "Moving cmdline-tools into cmdline-tools/latest (required by avdmanager)"
        mkdir -p "$latest"
        local item name
        for item in "$root/cmdline-tools"/*; do
            name=$(basename "$item")
            [ "$name" = "latest" ] && continue
            mv "$item" "$latest/"
        done
    fi
}

sdkmanager_bin() {
    local root="${ANDROID_HOME:-$ANDROID_SDK_PATH}"
    if [ -x "$root/cmdline-tools/latest/bin/sdkmanager" ]; then
        echo "$root/cmdline-tools/latest/bin/sdkmanager"
    elif [ -x "$root/cmdline-tools/bin/sdkmanager" ]; then
        echo "$root/cmdline-tools/bin/sdkmanager"
    elif command_exists sdkmanager; then
        command -v sdkmanager
    else
        echo ""
    fi
}

avdmanager_bin() {
    local root="${ANDROID_HOME:-$ANDROID_SDK_PATH}"
    if [ -x "$root/cmdline-tools/latest/bin/avdmanager" ]; then
        echo "$root/cmdline-tools/latest/bin/avdmanager"
    elif [ -x "$root/cmdline-tools/bin/avdmanager" ]; then
        echo "$root/cmdline-tools/bin/avdmanager"
    else
        echo ""
    fi
}

ensure_sdk_package() {
    local pkg="$1"
    if sdk_pkg_present "$pkg"; then
        print_success "$pkg already present — skip download"
        return 0
    fi
    local sm
    sm=$(sdkmanager_bin)
    if [ -z "$sm" ]; then
        print_warning "sdkmanager not found; cannot install $pkg"
        return 1
    fi
    print_info "Installing $pkg"
    yes | "$sm" --sdk_root="${ANDROID_HOME:-$ANDROID_SDK_PATH}" "$pkg"
}

install_cmdline_tools() {
    local root="$ANDROID_SDK_PATH"
    if [ -x "$root/cmdline-tools/latest/bin/sdkmanager" ] || [ -x "$root/cmdline-tools/bin/sdkmanager" ]; then
        print_success "Command-line tools already installed — skip zip download"
        ensure_cmdline_tools_layout
        return 0
    fi

    local zip_name url
    if [ "$OS_NAME" == "macOS" ]; then
        zip_name="commandlinetools-mac-${CMDLINE_TOOLS_REV}_latest.zip"
    else
        zip_name="commandlinetools-linux-${CMDLINE_TOOLS_REV}_latest.zip"
    fi
    url="https://dl.google.com/android/repository/$zip_name"

    local archive
    if ! archive=$(download_cached "$url" 10000000); then
        wait_for_manual_install "sdkmanager" "https://developer.android.com/studio#command-tools"
        return 1
    fi

    local temp_dir
    temp_dir=$(mktemp -d)
    unzip -q "$archive" -d "$temp_dir"
    mkdir -p "$root/cmdline-tools/latest"
    if [ -d "$temp_dir/cmdline-tools" ]; then
        cp -a "$temp_dir/cmdline-tools/." "$root/cmdline-tools/latest/"
    else
        cp -a "$temp_dir/." "$root/cmdline-tools/latest/"
    fi
    rm -rf "$temp_dir"
    print_success "Android SDK Command Line Tools installed under cmdline-tools/latest"
}

# ============================================================================
# Step 1: Check Java Installation
# ============================================================================
echo -e "${BLUE}Step 1: Checking Java installation...${NC}"

JAVA_INSTALLED=false
if command_exists java; then
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
    print_success "Java found: $JAVA_VERSION"

    if [[ "$JAVA_VERSION" == 17.* ]]; then
        print_success "Java version is 17 (required)"
        JAVA_INSTALLED=true
    else
        print_warning "Java version is $JAVA_VERSION, but version 17 is recommended"
        if ask_yes_no "Install Java 17?" "y"; then
            print_info "Installing Java 17..."

            if [ "$OS_NAME" == "macOS" ]; then
                if has_homebrew; then
                    brew install openjdk@17
                    print_success "Java 17 installed via Homebrew"
                    JAVA_INSTALLED=true
                else
                    wait_for_manual_install "java" "https://adoptium.net/temurin/releases?version=17"
                fi
            else
                if [ "$(id -u)" -eq 0 ]; then
                    apt update && apt install -y openjdk-17-jdk
                    JAVA_INSTALLED=true
                elif sudo apt update && sudo apt install -y openjdk-17-jdk; then
                    print_success "Java 17 installed"
                    JAVA_INSTALLED=true
                else
                    wait_for_manual_install "java" "https://adoptium.net/temurin/releases?version=17"
                fi
            fi
        else
            print_info "Skipping Java installation"
        fi
    fi
else
    print_warning "Java not found in PATH"
    if ask_yes_no "Install Java 17?" "y"; then
        print_info "Installing Java 17..."

        if [ "$OS_NAME" == "macOS" ]; then
            if has_homebrew; then
                brew install openjdk@17
                print_success "Java 17 installed via Homebrew"
                JAVA_INSTALLED=true
            else
                wait_for_manual_install "java" "https://adoptium.net/temurin/releases?version=17"
            fi
        else
            if [ "$(id -u)" -eq 0 ]; then
                apt update && apt install -y openjdk-17-jdk
                JAVA_INSTALLED=true
            elif sudo apt update && sudo apt install -y openjdk-17-jdk; then
                print_success "Java 17 installed"
                JAVA_INSTALLED=true
            else
                wait_for_manual_install "java" "https://adoptium.net/temurin/releases?version=17"
            fi
        fi
    else
        print_info "Skipping Java installation"
    fi
fi

echo ""

# ============================================================================
# Step 2: Android SDK root + command-line tools (one zip, cached)
# ============================================================================
echo -e "${BLUE}Step 2: Checking Android SDK...${NC}"

ANDROID_SDK_PATH="${ANDROID_HOME:-$HOME/android-dev-tools/android-sdk}"
mkdir -p "$ANDROID_SDK_PATH"

if [ -d "$ANDROID_SDK_PATH" ]; then
    print_success "Android SDK root: $ANDROID_SDK_PATH"
fi

if [ -x "$ANDROID_SDK_PATH/cmdline-tools/latest/bin/sdkmanager" ] || [ -x "$ANDROID_SDK_PATH/cmdline-tools/bin/sdkmanager" ]; then
    print_success "Command-line tools found"
    ensure_cmdline_tools_layout
else
    print_warning "Command-line tools not found"
    if ask_yes_no "Install Android SDK Command Line Tools?" "y"; then
        install_cmdline_tools
    fi
fi

echo ""

# ============================================================================
# Step 3: Environment variables
# ============================================================================
echo -e "${BLUE}Step 3: Setting up environment variables...${NC}"

if [ -z "$JAVA_HOME" ]; then
    print_info "JAVA_HOME not set, attempting to detect..."

    if [ "$OS_NAME" == "macOS" ]; then
        if [ -d "/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home" ]; then
            JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home"
        elif [ -d "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home" ]; then
            JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"
        elif [ -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
            JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
        elif [ -d "/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
            JAVA_HOME="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
        elif command_exists java; then
            JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || true)
        fi
    else
        if [ -d "/usr/lib/jvm/zulu17-ca-jdk-amd64" ]; then
            JAVA_HOME="/usr/lib/jvm/zulu17-ca-jdk-amd64"
        elif [ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]; then
            JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
        elif [ -d "/usr/lib/jvm/temurin-17-jdk-amd64" ]; then
            JAVA_HOME="/usr/lib/jvm/temurin-17-jdk-amd64"
        elif command_exists java; then
            JAVA_HOME=$(dirname "$(dirname "$(readlink "$(readlink "$(which java)")")")")
        fi
    fi

    if [ -n "$JAVA_HOME" ]; then
        print_success "Found Java at: $JAVA_HOME"
    else
        print_warning "Could not auto-detect Java installation"
    fi
else
    print_success "JAVA_HOME already set to: $JAVA_HOME"
fi

export ANDROID_HOME="$ANDROID_SDK_PATH"
export ANDROID_SDK_ROOT="$ANDROID_SDK_PATH"
print_success "ANDROID_HOME=$ANDROID_HOME"

ensure_cmdline_tools_layout

if [ -d "$ANDROID_HOME/cmdline-tools/latest/bin" ]; then
    export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
fi
if [ -d "$ANDROID_HOME/platform-tools" ]; then
    export PATH="$ANDROID_HOME/platform-tools:$PATH"
fi
if [ -d "$ANDROID_HOME/emulator" ]; then
    export PATH="$ANDROID_HOME/emulator:$PATH"
fi
if [ -n "$JAVA_HOME" ] && [ -d "$JAVA_HOME/bin" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi
print_success "PATH updated"

echo ""

# ============================================================================
# Step 4: SDK packages (skip any package already on disk)
# ============================================================================
echo -e "${BLUE}Step 4: Android SDK packages (API $ANDROID_API)...${NC}"

SM=$(sdkmanager_bin)
if [ -n "$SM" ]; then
    print_success "sdkmanager: $SM"
    print_info "Accepting licenses if needed (no package download)"
    yes | "$SM" --sdk_root="$ANDROID_HOME" --licenses >/dev/null || true

    ensure_sdk_package "platform-tools"
    ensure_sdk_package "platforms;android-${ANDROID_API}"
    ensure_sdk_package "build-tools;${BUILD_TOOLS_VER}"
    ensure_sdk_package "emulator"
    ensure_sdk_package "$(sdk_sysimage_pkg)"
else
    print_warning "sdkmanager not found — skip package install"
fi

echo ""

# ============================================================================
# Step 5: Local emulator AVD
# ============================================================================
echo -e "${BLUE}Step 5: Local emulator (AVD $AVD_NAME)...${NC}"

if [ -f "$HOME/.android/avd/${AVD_NAME}.ini" ]; then
    print_success "AVD $AVD_NAME already exists — skip create"
else
    AM=$(avdmanager_bin)
    if [ -z "$AM" ]; then
        print_warning "avdmanager not found; cannot create AVD"
    elif ask_yes_no "Create local emulator $AVD_NAME?" "y"; then
        print_info "Creating AVD $AVD_NAME ($(sdk_sysimage_pkg))"
        echo no | "$AM" create avd \
            --name "$AVD_NAME" \
            --package "$(sdk_sysimage_pkg)" \
            --device "$AVD_DEVICE" \
            --force
        print_success "AVD $AVD_NAME created"
    fi
fi

if command_exists adb || [ -x "$ANDROID_HOME/platform-tools/adb" ]; then
    print_success "adb available (SDK platform-tools — no extra zip)"
else
    print_warning "adb not found after platform-tools install"
fi

echo ""

# ============================================================================
# Step 6: Gradle wrapper
# ============================================================================
echo -e "${BLUE}Step 6: Checking Gradle...${NC}"

if [ -f "gradlew" ]; then
    print_success "Gradle wrapper script found (gradlew)"
    if [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
        print_success "Gradle wrapper JAR found"
    else
        print_warning "gradle/wrapper/gradle-wrapper.jar missing"
    fi
else
    print_warning "gradlew not found in the current directory"
    print_info "Run this script from the repository root"
fi

echo ""

# ============================================================================
# Step 7: local.properties
# ============================================================================
echo -e "${BLUE}Step 7: Checking local.properties...${NC}"

if [ ! -f "local.properties" ]; then
    {
        echo "# Android SDK path - auto-generated by setup.sh"
        echo "sdk.dir=$ANDROID_HOME"
    } > local.properties
    print_success "local.properties created"
else
    print_success "local.properties already exists"
    CURRENT_SDK=$(grep -E "^sdk\.dir=" local.properties 2>/dev/null | cut -d'=' -f2)
    if [ "$CURRENT_SDK" != "$ANDROID_HOME" ]; then
        print_warning "Current sdk.dir ($CURRENT_SDK) differs from ANDROID_HOME ($ANDROID_HOME)"
        if ask_yes_no "Update local.properties with correct SDK path?" "y"; then
            sed -i.bak "s|^sdk\.dir=.*|sdk.dir=$ANDROID_HOME|" local.properties
            rm -f local.properties.bak
            print_success "local.properties updated"
        fi
    else
        print_success "sdk.dir is correctly set"
    fi
fi

echo ""

# ============================================================================
# Summary
# ============================================================================
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Environment Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
if [ -n "$JAVA_HOME" ]; then
    echo -e "JAVA_HOME=$JAVA_HOME"
else
    echo -e "JAVA_HOME=${YELLOW}NOT SET (install Java 17 and set JAVA_HOME)${NC}"
fi
echo -e "ANDROID_HOME=$ANDROID_HOME"
echo -e "AVD=$AVD_NAME"
echo ""
echo -e "To build:  ${GREEN}./gradlew assembleDebug${NC}"
echo -e "To deploy: ${GREEN}./scripts/deploy.sh debug${NC}"
echo -e "  (starts $AVD_NAME if no device is connected)"
echo ""
echo -e "Re-running this script skips every zip and SDK package already on disk."
echo -e "Cached archives live in ${GREEN}$CACHE_DIR${NC}."
echo ""
echo -e "To make environment variables permanent, add:"
echo ""
if [ -n "$JAVA_HOME" ]; then
    echo "export JAVA_HOME=$JAVA_HOME"
fi
echo "export ANDROID_HOME=$ANDROID_HOME"
echo "export ANDROID_SDK_ROOT=\$ANDROID_HOME"
echo 'export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"'
echo ""
