#!/bin/sh

POM_FILE="pom.xml"
LICENSES_FILE="target/generated-resources/licenses.xml"
OUTPUT_FILE="src/main/resources/backend-deps.csv"

# Output header
echo "groupId,artifactId,version,license,licenseUrl" > "$OUTPUT_FILE"

COUNT=$(xmllint --xpath "count(//*[local-name()='dependencies']/*[local-name()='dependency'])" "$POM_FILE" 2>/dev/null)

# Loop through each dependency using index
for i in $(seq 1 "$COUNT"); do
    groupId=$(xmllint --xpath "string((//*[local-name()='dependencies']/*[local-name()='dependency'])[$i]/*[local-name()='groupId'])" "$POM_FILE" 2>/dev/null)
    artifactId=$(xmllint --xpath "string((//*[local-name()='dependencies']/*[local-name()='dependency'])[$i]/*[local-name()='artifactId'])" "$POM_FILE" 2>/dev/null)
    version=$(xmllint --xpath "string((//*[local-name()='dependencies']/*[local-name()='dependency'])[$i]/*[local-name()='version'])" "$POM_FILE" 2>/dev/null)
    license=$(xmllint --xpath "string(//*[local-name()='dependency'][*[local-name()='groupId']='$groupId' and *[local-name()='artifactId']='$artifactId']/*[local-name()='licenses']/*[local-name()='license'][1]/*[local-name()='name'])" "$LICENSES_FILE" 2>/dev/null)
    #license=$(xmllint --xpath "string((//*[local-name()='dependencies']/*[local-name()='dependency'])[$i]/*[local-name()='name'])" "$POM_FILE" 2>/dev/null)
    # Default to N/A if version is missing

    # XPath to find license URL for matching dep in licenses.xml
    licenseUrl=$(xmllint --xpath "string(//*[local-name()='dependency'][*[local-name()='groupId']='$groupId' and *[local-name()='artifactId']='$artifactId']/*[local-name()='licenses']/*[local-name()='license'][1]/*[local-name()='url'])" "$LICENSES_FILE" 2>/dev/null)
    licenseUrl=${licenseUrl:-N/A}

    echo "$groupId;$artifactId;$version;$license;$licenseUrl" >> "$OUTPUT_FILE"
done

echo "✅ Extracted $COUNT dependencies to $OUTPUT_FILE"