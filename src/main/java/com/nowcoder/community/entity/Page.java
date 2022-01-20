package com.nowcoder.community.entity;


import lombok.Data;
import lombok.ToString;

/**
 * 封装分页相关的信息.
 */
@ToString
public class Page {
    //当前页数
    private int currentPage = 1;
    //limit
    private int limit = 10;
    // 数据总数(用于计算总页数)
    private int rows;
    // 查询路径(用于复用分页链接)
    private String path;    //[/index]是请求路径

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        if (currentPage>=1){
            this.currentPage = currentPage;
        }
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        if(limit>=1 && limit<=50){
            this.limit = limit;
        }
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        if(rows>=0){
            this.rows = rows;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    //总【页面数】
    public int getTotal(){
        return (rows/limit) + ((rows%limit==0)?0:1);
    }

    //得到起始条数
    public int getOffset(){
        return (currentPage-1)*limit;
    }

    //显示的开始页
    public int getFrom(){
        int from = currentPage - 2;
        return from>=1 ? from : 1;
    }

    //显示的结尾页
    public int getTo(){
        int to = currentPage + 2;
        int total = getTotal();
        return to <= total ? to : total;
    }
}
