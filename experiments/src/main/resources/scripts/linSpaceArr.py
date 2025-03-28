import sys
import numpy as np

def main():
    start = int(sys.argv[1])
    stop = int(sys.argv[2])
    num = int(sys.argv[3])
    result = ""
    for x in np.linspace(start, stop, num):
        result += f"{str(int(x))},"
    print(f"{result[:-1]}", end='')

if __name__ == "__main__":
    main()
