import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np
import argparse
import io

def main(args):
    # Parse specified input files
    parsedFiles = [] # shape of items: [dataX, dataY1, dataY2, color, label]
    for inpFile in args.files:
        try:
            content = open(inpFile, "r").read()
            contentLines = content.splitlines()
            color = contentLines[10][10:-1]
            label = contentLines[11][10:-1]
            contentF = io.StringIO(content)
            arr = np.loadtxt(contentF, delimiter=",", skiprows=14)
            dataX = arr[:,0]
            dataY1 = arr[:,-2]
            dataY2 = arr[:,-1]
            parsedFiles.append([dataX, dataY1, dataY2, color, label])
        except Exception as e:
            print(f"An exception occurred during the parsing of {inpFile}!\n{e}")

    # Plotting
    plt.rcParams["font.size"] = 14
    if args.rm_mem_plot:
        fig, ax1 = plt.subplots(1, figsize=(args.width, args.height))
        ax1 = [ax1]
    else:
        fig, ax1 = plt.subplots(2, figsize=(args.width, args.height), gridspec_kw={'height_ratios': [2, 1]}, sharex=True)
    ax1[0].set_ylabel("Dauer [s]")
    ax1[0].yaxis.set_major_locator(ticker.MaxNLocator(nbins=10))
    ax1[0].grid(axis="y")
    if not args.rm_mem_plot:
        ax1[1].set_xlabel(args.x_label.replace('"', ''))
        ax1[1].set_ylabel("Max. Speicher-\nbedarf [GB]")
        ax1[1].yaxis.set_major_locator(ticker.MaxNLocator(nbins=5))
        ax1[1].grid(axis="y")
    else:
        ax1[0].set_xlabel(args.x_label.replace('"', ''))

    for parsedFile in parsedFiles:
        ax1[0].plot(parsedFile[0], parsedFile[1], color=parsedFile[3], marker="o", label=parsedFile[4], lw=2, markersize=6)
        if not args.rm_mem_plot:
            addPlotToLower(ax1[1], parsedFile[0], parsedFile[2], parsedFile[3], parsedFile[4])

    legend = ax1[0].legend(loc=args.legend_pos.replace('"', ''), framealpha=1)
    legend.get_frame().set_edgecolor("black")
    fig.suptitle(args.title.replace('"', ''))
    fig.tight_layout()
    if args.save == None:
        plt.show()
    else:
        plt.savefig(args.save, dpi=300)

def addPlotToLower(ax, dataX, dataY, color, label):
    entriesToBeRemoved = [ i for i, elem in enumerate(dataY) if elem == -1 ]
    newDataX = np.delete(dataX, entriesToBeRemoved)
    newDataY = np.delete(dataY, entriesToBeRemoved)
    ax.plot(newDataX, newDataY, color=color, linestyle='-', label=label, lw=2)

if __name__ == "__main__":
    ap = argparse.ArgumentParser(description="Program for plotting the performance of SMT-Solvers")
    ap.add_argument("files", type=str, nargs="+", help="Paths to the experiment files")
    ap.add_argument("-W", "--width", type=int, default=9, help="Width of the plot")
    ap.add_argument("-H", "--height", type=int, default=7, help="Height of the plot")
    ap.add_argument("--rm_mem_plot", action='store_true', help="Removes memory part from plot")
    ap.add_argument("--title", type=str, default="", help="Title of the plot")
    ap.add_argument("--x_label", type=str, default="", help="Label at the x-axis of the plot")
    ap.add_argument("--legend_pos", type=str, default="upper left", help="Position of legend")
    ap.add_argument("-S", "--save", type=str, help="Saves the plot to the specified file")
    args = ap.parse_args()
    main(args)