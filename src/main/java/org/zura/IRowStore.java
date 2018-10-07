package org.zura.JournalFilter;

interface IRowStore {
    public void storeRow(Integer no, String timeStamp, String fileName, String fullPath, String eventInfo, String fileAttr);
    public void close();
}
