data <- read.table("all_urls.txt", stringsAsFactors=F)
data.unique <- unique(data)

domains <- sapply(data.unique[,1], function(x){
  s <- gsub(":80", "", x)
  strsplit(s, "/")[[1]][3]
})

hist(table(domains))

domains.sample <-sample(domains,1000)
unique(domains.sample)

data.sample <- data.unique[sample(nrow(data.unique), 1000),]

write.table(data.sample[,1],"url_sample.txt",row.names=F, col.names=F)
