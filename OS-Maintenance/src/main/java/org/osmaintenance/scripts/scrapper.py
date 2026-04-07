import platform
import subprocess
import requests
import xml.etree.ElementTree as ET
from ctypes import windll, create_unicode_buffer
import os
import warnings
warnings.simplefilter("ignore")

def get_installed_driver_version():
    try:
        output = subprocess.check_output(['nvidia-smi', '--query-gpu=driver_version', '--format=csv,noheader'], encoding='utf-8')
        return output.strip()
    except:
        return None

def get_latest_driver_version(model, osID):
    # Get model-specific ParentID and Value
    lookup_url = "https://www.nvidia.com/Download/API/lookupValueSearch.aspx?TypeID=3"
    xml_data = requests.get(lookup_url).content
    root = ET.fromstring(xml_data)

    parent_id = None
    value = None
    for val in root.iter('LookupValue'):
        name_elem = val.find('Name')
        parent_elem = val.get('ParentID')
        value_elem = val.find('Value')

        name = name_elem.text
        if name is None or value_elem is None or parent_elem is None:
            continue

        if name and model.lower() in name.lower():
            parent_id = parent_elem
            #print(f"Found ParentID: {parent_id} for model: {model}")
            value = value_elem.text
            #print(f"Found Value: {value} for model: {model}")
            break


    # Now query the Ajax API
    api_url = f"https://gfwsl.geforce.com/services_toolkit/services/com/nvidia/services/AjaxDriverService.php"
    params = {
        'func': 'DriverManualLookup',
        'psid': parent_id,
        'pfid': value,
        'osID': osID,  # Win 10/11 x64
        'languageCode': '1033',
        'dch': '1',
        'upCRD': '0'
    }
    response = requests.get(api_url, params=params)
    data = response.json()
    version = data['IDS'][0]['downloadInfo']['Version']
    url = data['IDS'][0]['downloadInfo']['DownloadURL']
    return version, url

def getUpdateInfo(model):
    if platform.system() != "Windows" or platform.machine() != "AMD64":
        return None, None

    current_version = get_installed_driver_version()
    if not current_version:
        return None, None

    arch = platform.architecture()[0]
    if arch == '64bit':
        osID = '57'  # Win 10/11 x64
    else:
        osID = '56'  # Win 10/11 x86

    latest_version, download_url = get_latest_driver_version(model, osID)
    if latest_version and download_url:
        return current_version, latest_version, download_url
    else:
        return current_version, None, None

if __name__ == "__main__":
    import sys
    model = sys.argv[1] if len(sys.argv) > 1 else "GTX 1650"
    result = getUpdateInfo(model)
    if result:
        print("\t".join([x if x else "" for x in result]))
    else:
        print("\t\t")