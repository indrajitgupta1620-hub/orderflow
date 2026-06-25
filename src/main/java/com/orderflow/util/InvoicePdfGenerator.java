package com.orderflow.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.orderflow.model.Order;
import com.orderflow.model.OrderItem;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

public class InvoicePdfGenerator {

    public static byte[] generate(Order order, String invoiceNumber) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Font.BOLD);
            Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font regularBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD);
            
            Paragraph title = new Paragraph("INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            Paragraph meta = new Paragraph();
            meta.add(new Chunk("Invoice Number: " + invoiceNumber + "\n", regularBold));
            meta.add(new Chunk("Date: " + order.getPlacedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\n", regularFont));
            meta.add(new Chunk("Order ID: #" + order.getId() + "\n", regularFont));
            meta.add(new Chunk("Customer: " + order.getUser().getUsername() + " (" + order.getUser().getEmail() + ")\n", regularFont));
            if (order.getUser().getPhone() != null && !order.getUser().getPhone().trim().isEmpty()) {
                meta.add(new Chunk("Phone: " + order.getUser().getPhone() + "\n", regularFont));
            }
            meta.setSpacingAfter(15);
            document.add(meta);
            
            Paragraph address = new Paragraph();
            address.add(new Chunk("Shipping Address:\n", headingFont));
            address.add(new Chunk(order.getShippingAddress() + "\n", regularFont));
            if (order.getNotes() != null && !order.getNotes().trim().isEmpty()) {
                address.add(new Chunk("Notes: " + order.getNotes() + "\n", regularFont));
            }
            address.setSpacingAfter(20);
            document.add(address);
            
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{5, 2, 1, 2});
            
            PdfPCell cell;
            
            cell = new PdfPCell(new Phrase("Product Name", regularBold));
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
            
            cell = new PdfPCell(new Phrase("Unit Price (INR)", regularBold));
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cell);
            
            cell = new PdfPCell(new Phrase("Qty", regularBold));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
            
            cell = new PdfPCell(new Phrase("Subtotal (INR)", regularBold));
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cell);
            
            for (OrderItem item : order.getOrderItems()) {
                table.addCell(new Phrase(item.getProduct().getName(), regularFont));
                
                cell = new PdfPCell(new Phrase(item.getUnitPrice().toString(), regularFont));
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cell);
                
                cell = new PdfPCell(new Phrase(item.getQuantity().toString(), regularFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
                
                cell = new PdfPCell(new Phrase(item.getSubtotal().toString(), regularFont));
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cell);
            }
            
            document.add(table);
            
            Paragraph total = new Paragraph();
            total.setAlignment(Element.ALIGN_RIGHT);
            total.add(new Chunk("\nTotal Amount: INR " + order.getTotalAmount().toString(), headingFont));
            document.add(total);
            
            Paragraph footer = new Paragraph("\n\nThank you for ordering with OrderFlow!", regularFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
            
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return out.toByteArray();
    }
}
