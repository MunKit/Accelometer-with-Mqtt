import xlwt


wb = xlwt.Workbook()

ws = wb.add_sheet('rawdata move-x')
row = 1
col = 0
txtfile = open("accumulate distance.txt","r")
for line in txtfile:
    print line
    while True: 
        index = line.find(" ")
        if index == -1:
            ws.write(row,col, line[:-2])
            col = 0
            break;
        ws.write(row,col, line[:index])
        col+=1
        line = line[index+1:]
    row += 1
        
    
wb.save('example1.xls')
