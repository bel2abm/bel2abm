#!/usr/bin/env Rscript

# R code to evaluate the SORLA results
# input: folder structure
#		myfolder_whatevername (with subfolders for the concentrations; need to be the same as in the conc_folder vector;
#			each needs to contain at least numberofreplicateruns files with name inputdata; also recursive reading of data)
#			10app
#			50app
#			100app
#			500app
# output: plot of x axis: APP concentrations / numbers; y axis: number of sapp molecules present
############# settings and parameters ##########################

conc_folder =
  c("10app",
    "20app",
    "50app",
    "100app",
    "200app",
    "300app",
    "400app",
    "500app",
    "600app")
xaxis = c(10, 20, 50, 100, 200, 300, 400, 500, 600) # for plotting

numberofreplicateruns = 400
# the code will stop once numberofreplicateruns number of valid files have been processed

attick = 10
################################################################

args = commandArgs(trailingOnly=TRUE)

if (length(args) == 0) {
  folder = getwd()
} else if (length(args) == 1) {
  folder = args[1] # Either use wo_SORLA_22secr or with_3300SORLA_22secr folder
}

mean_sappa_values = NULL
mean_sappb_values = NULL
mean_sappad_values = NULL
mean_sappbd_values = NULL
pb = txtProgressBar(min = 0,
                    max = length(conc_folder),
                    style = 3) #text based bar
counter = 1
for (fold in conc_folder) {
  # progress bar
  Sys.sleep(0.1)
  setTxtProgressBar(pb, counter)
  counter = counter + 1
  
  file_list =
    list.files(
      path = paste(folder, fold, sep = "/"),
      pattern = "\\.txt$",
      recursive = T
    )
  runcounter = numberofreplicateruns
  readdata_sappa = NULL
  readdata_sappb = NULL
  readdata_sappa_d = NULL
  readdata_sappb_d = NULL
  for (file in file_list) {
    # read as a data.frame; this reads the data for 1 concentration (folder)
    data =
      read.table(
        paste(folder, fold, file, sep = "/"),
        sep = " ",
        header = TRUE,
        fill = FALSE,
        stringsAsFactors = FALSE,
        check.names = TRUE
      )
    runcounter = runcounter - 1
    if (runcounter == 0) {
      mean_sappa_values = c(mean_sappa_values, mean(readdata_sappa))
      mean_sappb_values = c(mean_sappb_values, mean(readdata_sappb))
      mean_sappad_values =
        c(mean_sappad_values, mean(readdata_sappa_d))
      mean_sappbd_values =
        c(mean_sappbd_values, mean(readdata_sappb_d))
      break
    }
    # save all data; at the end of the loop, calculate mean value and save it for plotting
    readdata_sappa = c(readdata_sappa, data[with(data, tick == attick),]$sappalphas)
    readdata_sappb = c(readdata_sappb, data[with(data, tick == attick),]$sappbetas)
    readdata_sappa_d = c(readdata_sappa_d, data[with(data, tick == attick),]$sappalpha_ds)
    readdata_sappb_d = c(readdata_sappb_d, data[with(data, tick == attick),]$sappbeta_ds)
  }
  if (runcounter > 0) {
    print(
      paste(
        "attention: number of input files in ",
        fold,
        " insuffient! ",
        runcounter,
        " further valid files needed."
      )
    )
  }
}
close(pb)

##plot mean values mean_sappa_values etc values als y achse, xaxis names als x achse
pdf(paste(folder, "sappa.pdf", sep=""))
sappa_sums = apply(rbind(mean_sappa_values, mean_sappad_values), 2, sum)
plot(
  xaxis,
  mean_sappa_values,
  cex.main = 0.8,
  ylim = c(0, max(sappa_sums)),
  main = "sAPP alpha initial production rates [delta product/delta time_interval_initial]",
  xlab = "APP",
  ylab = "delta sAPP/delta initial time period [10 time points]",
  type = "l",
  col = "red"
)
lines(xaxis, mean_sappad_values, col = "green")
lines(xaxis, sappa_sums, col = "black")
legend(
  400,
  1,
  legend = c("sAPPa", "sAPPa_d", "sum"),
  col = c("red", "green", "black"),
  lty = 1:2,
  cex = 0.8
)
dev.off()

pdf(paste(folder, "sappb.pdf", sep=""))
sappb_sums = apply(rbind(mean_sappb_values, mean_sappbd_values), 2, sum)
plot(
  xaxis,
  mean_sappb_values,
  cex.main = 0.8,
  ylim = c(0, max(sappb_sums)),
  main = "sAPP beta initial production rates [delta product/delta time_interval_initial]",
  xlab = "APP",
  ylab = "delta sAPP/delta initial time period [10 time points]",
  type = "l",
  col = "red"
)
lines(xaxis, mean_sappbd_values, col = "green")
lines(xaxis, sappb_sums, col = "black")

legend(
  400,
  0.1,
  legend = c("sAPPb", "sAPPb_d", "sum"),
  col = c("red", "green", "black"),
  lty = 1:2,
  cex = 0.8
)
dev.off()

df = data.frame(
  mean_sappa_values,
  mean_sappad_values,
  sappa_sums,
  mean_sappb_values,
  mean_sappbd_values,
  sappb_sums
)

rownames(df) = xaxis

table_out_path = paste(folder, 'results.tsv', sep="")
write.table(df, file = table_out_path, sep="\t")
