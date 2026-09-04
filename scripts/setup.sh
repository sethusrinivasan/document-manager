#!/bin/bash
# Development environment setup script for Document Manager
# Works on Ubuntu (Linux) and macOS

# Check if script is being run with bash
if [ -z "$BASH_VERSION" ]; then
    echo -e "\033[0;31mERROR: This script must be run with bash!\033[0m"
    echo ""
    echo "Please run the script with the following command:"
    echo "  \033[0;34mbash scripts/setup.sh\033[0m"
    echo ""
    echo "Or make it executable and run directly:"
    echo "  \033[0;34mchmod +x scripts/setup.sh && ./scripts/setup.sh\033[0m"
    echo ""
    echo "The script uses bash-specific features (like [[ ]] syntax)"
    echo "that are not available in other shells (dash, sh, etc.)"
    echo ""
    exit 1
fi

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Detect OS
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
echo -e "${BLUE}  Document Manager Setup${NC}"
echo -e "${BLUE}  OS: $OS_NAME${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to check if command exists
command_exists() {
    command -v "$1" &> /dev/null
}

# Function to check if package manager commands exist
has_package_manager() {
    if command_exists apt || command_exists apt-get; then
        echo "apt"
    elif command_exists brew; then
        echo "brew"
    else
        echo ""
    fi
}

# Function to check if homebrew is installed
has_homebrew() {
    command_exists brew
}

# Function to check if curl/wget are available
has_curl() {
    command_exists curl
}

has_wget() {
    command_exists wget
}

# Function to print success message
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Function to print warning message
print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Function to print info message
print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Function to prompt user for yes/no
ask_yes_no() {
    local prompt="$1"
    local default="${2:-y}"
    
    while true; do
        if [ "$default" == "y" ]; then
            echo -n -e "${BLUE}$prompt [Y/n]: ${NC}"
        else
            echo -n -e "${BLUE}$prompt [y/N]: ${NC}"
        fi
        
        read -r answer
        answer="${answer:-$default}"
        
        case "$answer" in
            [Yy]*)
                return 0
                ;;
            [Nn]*)
                return 1
                ;;
            *)
                echo -e "${RED}Please answer yes (y) or no (n)${NC}"
                ;;
        esac
    done
}

# Function to wait for manual installation
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
    echo -e "  ${BLUE}After downloading:${NC}"
    echo "    1. Extract the archive to the appropriate location"
    echo "    2. Make sure the binaries are in your PATH"
    echo ""
    echo -e "${YELLOW}Once installed, press Enter to continue...${NC}"
    
    # Wait for user input
    read -r
    
    # Check if tool is now available
    if command_exists "$tool_name"; then
        print_success "${tool_name} is now available"
        return 0
    else
        print_warning "${tool_name} not found in PATH"
        echo -e "${YELLOW}Please verify the installation and ensure the tools are in your PATH.${NC}"
        return 1
    fi
}

# Function to check if a file is cached and up to date
is_cached() {
    local file_path="$1"
    local min_size="$2"
    
    if [ -f "$file_path" ] && [ -s "$file_path" ]; then
        local file_size=$(stat -c%s "$file_path" 2>/dev/null || stat -f%z "$file_path" 2>/dev/null)
        if [ "$file_size" -ge "$min_size" ]; then
            return 0
        fi
    fi
    return 1
}

# Function to get cached download path
get_cached_path() {
    local url="$1"
    local basename=$(basename "$url")
    echo "$HOME/.kiro/cache/$basename"
}

# ============================================================================
# Step 1: Check Java Installation
# ============================================================================
echo -e "${BLUE}Step 1: Checking Java installation...${NC}"

JAVA_INSTALLED=false
if command_exists java; then
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
    print_success "Java found: $JAVA_VERSION"
    
    # Check if it's Java 17
    if [[ "$JAVA_VERSION" == 17.* ]]; then
        print_success "Java version is 17 (required)"
        JAVA_INSTALLED=true
    else
        print_warning "Java version is $JAVA_VERSION, but version 17 is recommended"
        if ask_yes_no "Install Java 17?" "y"; then
            print_info "Installing Java 17..."
            
            if [ "$OS_NAME" == "macOS" ]; then
                if has_homebrew; then
                    echo "Using Homebrew to install Java 17..."
                    if brew install openjdk@17; then
                        print_success "Java 17 installed via Homebrew"
                        JAVA_INSTALLED=true
                    else
                        echo -e "${RED}Failed to install Java via Homebrew${NC}"
                        wait_for_manual_install "java" "https://adoptium.net/temurin/releases?version=17"
                    fi
                else
                    echo -e "${RED}Homebrew not found. Please install Java 17 manually.${NC}"
                    echo ""
                    echo "  ${BLUE}Download URL:${NC} https://adoptium.net/temurin/releases?version=17"
                    echo "  ${BLUE}Or:${NC} brew install --cask temurin"
                    wait_for_manual_install "java" "https://adoptium.net/temurin/releases?version=17"
                fi
            else
                # Ubuntu
                if [ "$(id -u)" -eq 0 ]; then
                    apt update && apt install -y openjdk-17-jdk
                    JAVA_INSTALLED=true
                else
                    echo "Attempting to install Java 17 with sudo..."
                    if sudo apt update && sudo apt install -y openjdk-17-jdk; then
                        print_success "Java 17 installed"
                        JAVA_INSTALLED=true
                    else
                        echo -e "${RED}Failed to install Java${NC}"
                        wait_for_manual_install "java" "https://adoptium.net/temurin/releases?version=17"
                    fi
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
                echo "Using Homebrew to install Java 17..."
                if brew install openjdk@17; then
                    print_success "Java 17 installed via Homebrew"
                    JAVA_INSTALLED=true
                else
                    echo -e "${RED}Failed to install Java via Homebrew${NC}"
                    wait_for_manual_install "java" "https://adoptium.net/temurin/releases?version=17"
                fi
            else
                echo -e "${RED}Homebrew not found. Please install Java 17 manually.${NC}"
                echo ""
                echo "  ${BLUE}Download URL:${NC} https://adoptium.net/temurin/releases?version=17"
                echo "  ${BLUE}Or:${NC} brew install --cask temurin"
                wait_for_manual_install "java" "https://adoptium.net/temurin/releases?version=17"
            fi
        else
            # Ubuntu
            if [ "$(id -u)" -eq 0 ]; then
                apt update && apt install -y openjdk-17-jdk
                JAVA_INSTALLED=true
            else
                echo "Attempting to install Java 17 with sudo..."
                if sudo apt update && sudo apt install -y openjdk-17-jdk; then
                    print_success "Java 17 installed"
                    JAVA_INSTALLED=true
                else
                    echo -e "${RED}Failed to install Java${NC}"
                    wait_for_manual_install "java" "https://adoptium.net/temurin/releases?version=17"
                fi
            fi
        fi
    else
        print_info "Skipping Java installation"
    fi
fi

echo ""

# ============================================================================
# Step 2: Check Android SDK
# ============================================================================
echo -e "${BLUE}Step 2: Checking Android SDK...${NC}"

ANDROID_SDK_PATH="${ANDROID_HOME:-$HOME/android-dev-tools/android-sdk}"

if [ -d "$ANDROID_SDK_PATH" ]; then
    print_success "Android SDK found at: $ANDROID_SDK_PATH"
    
    # Check for required SDK components
    if [ -d "$ANDROID_SDK_PATH/platforms" ]; then
        if [ -d "$ANDROID_SDK_PATH/platforms/android-34" ]; then
            print_success "Android API 34 platform installed"
        else
            print_warning "Android API 34 platform NOT installed"
            if ask_yes_no "Install Android API 34?" "y"; then
                if command_exists sdkmanager; then
                    echo "Installing Android API 34..."
                    echo -e "y\n" | sdkmanager "platforms;android-34"
                    print_success "Android API 34 installed"
                else
                    print_info "sdkmanager not found. Install Android SDK Command Line Tools first."
                fi
            fi
        fi
    else
        print_warning "Android platforms directory NOT found"
    fi
    
    if [ -d "$ANDROID_SDK_PATH/build-tools" ]; then
        BUILD_TOOLS_VERSION=$(ls -1 "$ANDROID_SDK_PATH/build-tools" | sort -V | tail -1)
        print_success "Build tools found: $BUILD_TOOLS_VERSION"
        
        if [ -d "$ANDROID_SDK_PATH/build-tools/34.0.0" ]; then
            print_success "Build Tools 34.0.0 installed"
        else
            print_warning "Build Tools 34.0.0 NOT installed (recommended)"
            if ask_yes_no "Install Build Tools 34.0.0?" "y"; then
                if command_exists sdkmanager; then
                    echo "Installing Build Tools 34.0.0..."
                    echo -e "y\n" | sdkmanager "build-tools;34.0.0"
                    print_success "Build Tools 34.0.0 installed"
                else
                    print_info "sdkmanager not found. Install Android SDK Command Line Tools first."
                fi
            fi
        fi
    else
        print_warning "Build tools directory NOT found"
    fi
    
    if [ -d "$ANDROID_SDK_PATH/cmdline-tools" ]; then
        print_success "Command line tools installed"
    else
        print_warning "Command line tools directory NOT found"
    fi
    
else
    print_warning "Android SDK NOT found at: $ANDROID_SDK_PATH"
    if ask_yes_no "Install Android SDK?" "n"; then
        print_info "Downloading Android SDK Command Line Tools..."
        
        # Use the stable URL format
        if [ "$OS_NAME" == "macOS" ]; then
            SDK_ZIP="commandlinetools-mac-11076708_latest.zip"
            SDK_URL="https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip"
        else
            SDK_ZIP="commandlinetools-linux-11076708_latest.zip"
            SDK_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
        fi
        
        TEMP_DIR=$(mktemp -d)
        SDK_FILE="$TEMP_DIR/$SDK_ZIP"
        
        print_info "Downloading from: $SDK_URL"
        
        if has_curl; then
            if curl -L "$SDK_URL" -o "$SDK_FILE" --fail; then
                mkdir -p "$ANDROID_SDK_PATH/cmdline-tools"
                unzip -q "$SDK_FILE" -d "$TEMP_DIR"
                if [ -d "$TEMP_DIR/cmdline-tools" ]; then
                    # Copy contents instead of mv to avoid inter-device issues
                    rm -rf "$ANDROID_SDK_PATH/cmdline-tools"/*
                    cp -r "$TEMP_DIR/cmdline-tools"/* "$ANDROID_SDK_PATH/cmdline-tools/"
                else
                    rm -rf "$ANDROID_SDK_PATH/cmdline-tools"/*
                    cp -r "$TEMP_DIR"/* "$ANDROID_SDK_PATH/cmdline-tools/"
                fi
                rm -rf "$TEMP_DIR"
                print_success "Android SDK Command Line Tools installed"
                
                # Ask about installing sdkmanager packages
                if ask_yes_no "Install Android SDK packages (platforms, build tools) using sdkmanager?" "n"; then
                    if command_exists sdkmanager; then
                        echo "Installing Android SDK packages..."
                        echo -e "y\n" | sdkmanager "platforms;android-34" 2>/dev/null || print_warning "Failed to install platforms"
                        echo -e "y\n" | sdkmanager "build-tools;34.0.0" 2>/dev/null || print_warning "Failed to install build tools"
                    else
                        print_warning "sdkmanager not available"
                    fi
                fi
            else
                echo -e "${RED}Failed to download Android SDK${NC}"
                wait_for_manual_install "sdkmanager" "https://developer.android.com/studio#command-tools"
            fi
        elif has_wget; then
            if wget "$SDK_URL" -O "$SDK_FILE" --show-progress; then
                mkdir -p "$ANDROID_SDK_PATH/cmdline-tools"
                unzip -q "$SDK_FILE" -d "$TEMP_DIR"
                if [ -d "$TEMP_DIR/cmdline-tools" ]; then
                    # Copy contents instead of mv to avoid inter-device issues
                    rm -rf "$ANDROID_SDK_PATH/cmdline-tools"/*
                    cp -r "$TEMP_DIR/cmdline-tools"/* "$ANDROID_SDK_PATH/cmdline-tools/"
                else
                    rm -rf "$ANDROID_SDK_PATH/cmdline-tools"/*
                    cp -r "$TEMP_DIR"/* "$ANDROID_SDK_PATH/cmdline-tools/"
                fi
                rm -rf "$TEMP_DIR"
                print_success "Android SDK Command Line Tools installed"
            else
                echo -e "${RED}Failed to download Android SDK${NC}"
                wait_for_manual_install "sdkmanager" "https://developer.android.com/studio#command-tools"
            fi
        else
            echo -e "${RED}curl or wget required to download SDK${NC}"
            wait_for_manual_install "sdkmanager" "https://developer.android.com/studio#command-tools"
        fi
    fi
fi

echo ""

# ============================================================================
# Step 3: Set up environment variables
# ============================================================================
echo -e "${BLUE}Step 3: Setting up environment variables...${NC}"

# Find Java home if not set
if [ -z "$JAVA_HOME" ]; then
    print_info "JAVA_HOME not set, attempting to detect..."
    
    if [ "$OS_NAME" == "macOS" ]; then
        # Try to find Java 17 installed via Homebrew or zulu
        if [ -d "/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home" ]; then
            JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home"
            print_success "Found Zulu JDK 17 at: $JAVA_HOME"
        elif [ -d "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home" ]; then
            JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"
            print_success "Found Temurin JDK 17 at: $JAVA_HOME"
        elif [ -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
            JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
            print_success "Found OpenJDK 17 at: $JAVA_HOME"
        elif [ -d "/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
            JAVA_HOME="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
            print_success "Found OpenJDK 17 at: $JAVA_HOME"
        elif command_exists java; then
            JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null)
            if [ -n "$JAVA_HOME" ]; then
                print_success "Found Java 17 at: $JAVA_HOME"
            fi
        fi
    else
        # Ubuntu/Linux
        if [ -d "/usr/lib/jvm/zulu17-ca-jdk-amd64" ]; then
            JAVA_HOME="/usr/lib/jvm/zulu17-ca-jdk-amd64"
            print_success "Found Zulu JDK 17 at: $JAVA_HOME"
        elif [ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]; then
            JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
            print_success "Found OpenJDK 17 at: $JAVA_HOME"
        elif [ -d "/usr/lib/jvm/temurin-17-jdk-amd64" ]; then
            JAVA_HOME="/usr/lib/jvm/temurin-17-jdk-amd64"
            print_success "Found Temurin JDK 17 at: $JAVA_HOME"
        elif command_exists java; then
            JAVA_HOME=$(dirname $(dirname $(readlink $(readlink $(which java)))))
            print_success "Found Java at: $JAVA_HOME"
        fi
    fi
    
    if [ -z "$JAVA_HOME" ]; then
        print_warning "Could not auto-detect Java installation"
        print_info "Please set JAVA_HOME manually after installing Java"
    fi
else
    print_success "JAVA_HOME already set to: $JAVA_HOME"
fi

# Set up Android SDK path if not already set
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    print_info "ANDROID_HOME not set, using default: $ANDROID_SDK_PATH"
    export ANDROID_HOME="$ANDROID_SDK_PATH"
    export ANDROID_SDK_ROOT="$ANDROID_SDK_PATH"
    print_success "ANDROID_HOME set to: $ANDROID_HOME"
elif [ -d "$ANDROID_SDK_PATH" ]; then
    print_success "ANDROID_HOME found: $ANDROID_SDK_PATH"
else
    print_warning "ANDROID_HOME points to non-existent directory: $ANDROID_SDK_PATH"
    print_info "Please set ANDROID_HOME to your Android SDK location"
fi

# Update PATH with Android tools
echo ""
echo "Updating PATH..."

if [ -d "$ANDROID_HOME/cmdline-tools/latest/bin" ]; then
    export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
fi

if [ -d "$ANDROID_HOME/platform-tools" ]; then
    export PATH="$ANDROID_HOME/platform-tools:$PATH"
fi

if [ -d "$ANDROID_HOME/tools" ]; then
    export PATH="$ANDROID_HOME/tools:$PATH"
fi

if [ -d "$ANDROID_HOME/tools/bin" ]; then
    export PATH="$ANDROID_HOME/tools/bin:$PATH"
fi

if [ -n "$JAVA_HOME" ] && [ -d "$JAVA_HOME/bin" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

print_success "PATH updated"
echo ""

# ============================================================================
# Step 4: Check Android SDK Manager
# ============================================================================
echo -e "${BLUE}Step 4: Checking Android SDK Manager...${NC}"

if command_exists sdkmanager; then
    print_success "sdkmanager found"
    
    # Show installed packages
    print_info "Installed packages:"
    sdkmanager --list_installed 2>/dev/null | head -20 || print_warning "Could not list SDK packages"
else
    print_warning "sdkmanager not found"
    if ask_yes_no "Install Android SDK Command Line Tools?" "y"; then
        print_info "Downloading Android SDK Command Line Tools..."
        
        # Use the stable URL format
        if [ "$OS_NAME" == "macOS" ]; then
            SDK_ZIP="commandlinetools-mac-11076708_latest.zip"
            SDK_URL="https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip"
        else
            SDK_ZIP="commandlinetools-linux-11076708_latest.zip"
            SDK_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
        fi
        
        TEMP_DIR=$(mktemp -d)
        SDK_FILE="$TEMP_DIR/$SDK_ZIP"
        
        print_info "Downloading from: $SDK_URL"
        
        if has_curl; then
            if curl -L "$SDK_URL" -o "$SDK_FILE" --fail; then
                mkdir -p "$ANDROID_SDK_PATH/cmdline-tools"
                unzip -q "$SDK_FILE" -d "$TEMP_DIR"
                if [ -d "$TEMP_DIR/cmdline-tools" ]; then
                    # Copy contents instead of mv to avoid inter-device issues
                    rm -rf "$ANDROID_SDK_PATH/cmdline-tools"/*
                    cp -r "$TEMP_DIR/cmdline-tools"/* "$ANDROID_SDK_PATH/cmdline-tools/"
                else
                    rm -rf "$ANDROID_SDK_PATH/cmdline-tools"/*
                    cp -r "$TEMP_DIR"/* "$ANDROID_SDK_PATH/cmdline-tools/"
                fi
                rm -rf "$TEMP_DIR"
                print_success "Android SDK Command Line Tools installed"
            else
                echo -e "${RED}Failed to download Android SDK${NC}"
                wait_for_manual_install "sdkmanager" "https://developer.android.com/studio#command-tools"
            fi
        elif has_wget; then
            if wget "$SDK_URL" -O "$SDK_FILE" --show-progress; then
                mkdir -p "$ANDROID_SDK_PATH/cmdline-tools"
                unzip -q "$SDK_FILE" -d "$TEMP_DIR"
                if [ -d "$TEMP_DIR/cmdline-tools" ]; then
                    # Copy contents instead of mv to avoid inter-device issues
                    rm -rf "$ANDROID_SDK_PATH/cmdline-tools"/*
                    cp -r "$TEMP_DIR/cmdline-tools"/* "$ANDROID_SDK_PATH/cmdline-tools/"
                else
                    rm -rf "$ANDROID_SDK_PATH/cmdline-tools"/*
                    cp -r "$TEMP_DIR"/* "$ANDROID_SDK_PATH/cmdline-tools/"
                fi
                rm -rf "$TEMP_DIR"
                print_success "Android SDK Command Line Tools installed"
            else
                echo -e "${RED}Failed to download Android SDK${NC}"
                wait_for_manual_install "sdkmanager" "https://developer.android.com/studio#command-tools"
            fi
        else
            echo -e "${RED}curl or wget required to download SDK${NC}"
            wait_for_manual_install "sdkmanager" "https://developer.android.com/studio#command-tools"
        fi
    fi
fi

echo ""

# ============================================================================
# Step 5: Check ADB
# ============================================================================
echo -e "${BLUE}Step 5: Checking ADB...${NC}"

if command_exists adb; then
    ADB_VERSION=$(adb version 2>&1)
    print_success "ADB found: $(echo $ADB_VERSION | head -1)"
else
    print_warning "ADB not found in PATH"
    if ask_yes_no "Install Android Platform Tools (includes adb)?" "y"; then
        print_info "Downloading Android Platform Tools..."
        
        if [ "$OS_NAME" == "macOS" ]; then
            PLATFORM_TOOLS_ZIP="platform-tools-latest-darwin.zip"
            PLATFORM_URL="https://dl.google.com/android/repository/platform-tools-latest-darwin.zip"
        else
            PLATFORM_TOOLS_ZIP="platform-tools-latest-linux.zip"
            PLATFORM_URL="https://dl.google.com/android/repository/platform-tools-latest-linux.zip"
        fi
        
        TEMP_DIR=$(mktemp -d)
        PLATFORM_FILE="$TEMP_DIR/$PLATFORM_TOOLS_ZIP"
        
        print_info "Downloading from: $PLATFORM_URL"
        
        if has_curl; then
            if curl -L "$PLATFORM_URL" -o "$PLATFORM_FILE" --fail; then
                unzip -q "$PLATFORM_FILE" -d "$TEMP_DIR"
                
                # Copy adb to /usr/local/bin
                if [ -f "$TEMP_DIR/platform-tools/adb" ]; then
                    if [ "$(id -u)" -eq 0 ]; then
                        cp "$TEMP_DIR/platform-tools/adb" /usr/local/bin/
                    else
                        echo "Copying adb to /usr/local/bin with sudo..."
                        sudo cp "$TEMP_DIR/platform-tools/adb" /usr/local/bin/
                    fi
                fi
                
                # Copy fastboot if it exists
                if [ -f "$TEMP_DIR/platform-tools/fastboot" ]; then
                    if [ "$(id -u)" -eq 0 ]; then
                        cp "$TEMP_DIR/platform-tools/fastboot" /usr/local/bin/
                    else
                        sudo cp "$TEMP_DIR/platform-tools/fastboot" /usr/local/bin/ 2>/dev/null || true
                    fi
                elif [ -f "$TEMP_DIR/tools/fastboot" ]; then
                    if [ "$(id -u)" -eq 0 ]; then
                        cp "$TEMP_DIR/tools/fastboot" /usr/local/bin/
                    else
                        sudo cp "$TEMP_DIR/tools/fastboot" /usr/local/bin/ 2>/dev/null || true
                    fi
                fi
                
                rm -rf "$TEMP_DIR"
                print_success "Android Platform Tools installed"
            else
                echo -e "${RED}Failed to download Android Platform Tools${NC}"
                wait_for_manual_install "adb" "https://developer.android.com/tools#platform-tools"
            fi
        elif has_wget; then
            if wget "$PLATFORM_URL" -O "$PLATFORM_FILE" --show-progress; then
                unzip -q "$PLATFORM_FILE" -d "$TEMP_DIR"
                
                if [ -f "$TEMP_DIR/platform-tools/adb" ]; then
                    if [ "$(id -u)" -eq 0 ]; then
                        cp "$TEMP_DIR/platform-tools/adb" /usr/local/bin/
                    else
                        echo "Copying adb to /usr/local/bin with sudo..."
                        sudo cp "$TEMP_DIR/platform-tools/adb" /usr/local/bin/
                    fi
                fi
                
                if [ -f "$TEMP_DIR/platform-tools/fastboot" ]; then
                    if [ "$(id -u)" -eq 0 ]; then
                        cp "$TEMP_DIR/platform-tools/fastboot" /usr/local/bin/
                    else
                        sudo cp "$TEMP_DIR/platform-tools/fastboot" /usr/local/bin/ 2>/dev/null || true
                    fi
                elif [ -f "$TEMP_DIR/tools/fastboot" ]; then
                    if [ "$(id -u)" -eq 0 ]; then
                        cp "$TEMP_DIR/tools/fastboot" /usr/local/bin/
                    else
                        sudo cp "$TEMP_DIR/tools/fastboot" /usr/local/bin/ 2>/dev/null || true
                    fi
                fi
                
                rm -rf "$TEMP_DIR"
                print_success "Android Platform Tools installed"
            else
                echo -e "${RED}Failed to download Android Platform Tools${NC}"
                wait_for_manual_install "adb" "https://developer.android.com/tools#platform-tools"
            fi
        else
            echo -e "${RED}curl or wget required to download Platform Tools${NC}"
            wait_for_manual_install "adb" "https://developer.android.com/tools#platform-tools"
        fi
    fi
fi

echo ""

# ============================================================================
# Step 6: Check Gradle
# ============================================================================
echo -e "${BLUE}Step 6: Checking Gradle...${NC}"

GRADLE_WRAPPER_OK=false

# Check for gradlew script in project root
GRADLEW_SCRIPT="gradlew"
GRADLEW_JAR="gradle/wrapper/gradle-wrapper.jar"

if [ -f "$GRADLEW_SCRIPT" ]; then
    print_success "Gradle wrapper script found ($GRADLEW_SCRIPT)"
    
    if [ -f "$GRADLEW_JAR" ]; then
        print_success "Gradle wrapper JAR found ($GRADLEW_JAR)"
        
        # Test the wrapper
        if bash "$GRADLEW_SCRIPT" --version &> /dev/null; then
            print_success "Gradle wrapper is working"
            GRADLE_WRAPPER_OK=true
        else
            print_warning "Gradle wrapper test failed"
            print_info "This may be due to missing Java or Android SDK"
        fi
    else
        print_warning "Gradle wrapper JAR not found ($GRADLEW_JAR)"
        print_info "Gradle wrapper files are required but missing"
        print_info "The project should include gradlew and gradle/wrapper/gradle-wrapper.jar"
        print_info "You may need to initialize the Gradle wrapper:"
        print_info "  gradle wrapper --gradle-version 8.9"
    fi
else
    print_warning "Gradle wrapper script not found ($GRADLEW_SCRIPT)"
    print_info "Gradle wrapper files are required but missing"
    print_info "You may need to initialize the Gradle wrapper:"
    print_info "  gradle wrapper --gradle-version 8.9"
fi

echo ""

# ============================================================================
# Step 7: Create local.properties (if not exists)
# ============================================================================
echo -e "${BLUE}Step 7: Checking local.properties...${NC}"

if [ ! -f "local.properties" ]; then
    print_info "Creating local.properties..."
    
    {
        echo "# Android SDK path - auto-generated by setup.sh"
        echo "sdk.dir=$ANDROID_HOME"
    } > local.properties
    
    print_success "local.properties created"
else
    print_success "local.properties already exists"
    
    # Check if sdk.dir is set correctly
    CURRENT_SDK=$(grep -E "^sdk\.dir=" local.properties 2>/dev/null | cut -d'=' -f2)
    if [ "$CURRENT_SDK" != "$ANDROID_HOME" ]; then
        print_warning "Current sdk.dir ($CURRENT_SDK) differs from ANDROID_HOME ($ANDROID_HOME)"
        if ask_yes_no "Update local.properties with correct SDK path?" "y"; then
            sed -i.bak "s|^sdk\.dir=.*|sdk.dir=$ANDROID_HOME|" local.properties
            rm local.properties.bak
            print_success "local.properties updated"
        fi
    else
        print_success "sdk.dir is correctly set"
    fi
fi

echo ""

# ============================================================================
# Step 8: Display environment summary
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
echo ""

# Final summary
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Setup Complete!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "To build the project, run:"
echo -e "  ${GREEN}./gradlew assembleDebug${NC}"
echo ""
echo -e "To deploy to a connected device:"
echo -e "  ${GREEN}./scripts/deploy.sh${NC}"
echo ""
echo -e "Note: This script has set environment variables for this session."
echo -e "To make these changes permanent, add the following to your shell profile (~/.bashrc, ~/.zshrc, etc.):"
echo ""

if [ -n "$JAVA_HOME" ]; then
    echo "### Document Manager Dev Environment ###"
    echo "export JAVA_HOME=$JAVA_HOME"
    echo "export ANDROID_HOME=$ANDROID_HOME"
    echo "export ANDROID_SDK_ROOT=\$ANDROID_HOME"
    echo 'export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"'
    echo "### End Document Manager Setup ###"
else
    echo "### Document Manager Dev Environment ###"
    echo "# NOTE: Java was not detected. Please set JAVA_HOME to your JDK 17 installation."
    echo "# See instructions above for your OS."
    echo "export ANDROID_HOME=$ANDROID_HOME"
    echo "export ANDROID_SDK_ROOT=\$ANDROID_HOME"
    echo 'export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"'
    echo "### End Document Manager Setup ###"
fi
echo ""
