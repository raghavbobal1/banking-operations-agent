package com.bobal.banking;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OperationsControllerTest {
 @Autowired MockMvc mvc;
 @Test void health() throws Exception {mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.mode").value("synthetic-demo"));}
 @Test void accountLookup() throws Exception {mvc.perform(get("/api/accounts/ACC-1001")).andExpect(status().isOk()).andExpect(jsonPath("$.currency").value("CAD"));}
 @Test void missingTransaction() throws Exception {mvc.perform(get("/api/transactions/TX-9999")).andExpect(status().isNotFound());}
 @Test void existingCaseIsIdempotent() throws Exception {
  mvc.perform(post("/api/cases").contentType(MediaType.APPLICATION_JSON).content("{\"transactionId\":\"TX-2002\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("CASE-001"));
 }
 @Test void createsCaseOnce() throws Exception {
  String body="{\"transactionId\":\"TX-2003\"}";
  mvc.perform(post("/api/cases").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
  mvc.perform(post("/api/cases").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
 }
}