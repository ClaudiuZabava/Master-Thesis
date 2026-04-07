import gpustat

def gpuUsagePercentage():
    return gpustat.GPUStatCollection.new_query()[0].utilization

if __name__ == "__main__":
    # Call the function and print its result
    print(gpuUsagePercentage())