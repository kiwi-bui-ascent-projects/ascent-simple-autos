package com.galvanize.autos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galvanize.autos.exception.InvalidAutoException;
import com.galvanize.autos.model.*;
import com.galvanize.autos.service.AutosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AutosController.class)
public class AutosControllerTests {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    AutosService autosService;

    Automobile automobile;
    List<Automobile> autosList;
    ObjectMapper objectMapper;
    UpdateAutoRequest updateAutoRequest;

    @BeforeEach
    void setup() {
        autosList = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            Automobile automobile = new Automobile("Ford", "Mustang",
                    1967 + i, "ABC" + 12 + i);
            autosList.add(automobile);
        }

        automobile = new Automobile("Toyota", "Supra", 1995, "ABC321");
        updateAutoRequest = new UpdateAutoRequest(1234500, Preowned.CPO, Grade.EXCELLENT);
        objectMapper = new ObjectMapper();
    }

    @Test
    void getAutos_noArgs_exists_returnsAutosList() throws Exception {
        when(autosService.getAutos()).thenReturn(new AutosList(autosList));

        mockMvc.perform(get("/autos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.automobiles", hasSize(5)));
    }

    @Test
    void getAutos_noArgs_none_returnsNoContent() throws Exception {
        when(autosService.getAutos()).thenReturn(new AutosList());

        mockMvc.perform(get("/autos"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAutos_args_returnsAutosList() throws Exception {
        when(autosService.getAutos(anyString(), anyString())).thenReturn(new AutosList(autosList));

        mockMvc.perform(get("/autos?color=red&make=ford"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.automobiles", hasSize(5)));
    }

    @Test
    void postAuto_valid_returnsAuto() throws Exception {
        when(autosService.addAuto(any(Automobile.class))).thenReturn(automobile);

        mockMvc.perform(post("/autos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(automobile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.make").value("Toyota"));
    }

    @Test
    void postAuto_badRequest_returns400() throws Exception {
        when(autosService.addAuto(any(Automobile.class))).thenThrow(InvalidAutoException.class);

        mockMvc.perform(post("/autos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(automobile)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAuto_withVin_returnsAuto() throws Exception {
        when(autosService.getAuto(anyString())).thenReturn(automobile);

        mockMvc.perform(get("/autos/"+automobile.getVin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("vin").value(automobile.getVin()));
    }

    @Test
    void getAuto_vinNotFound_returns204() throws Exception {
        when(autosService.getAuto(anyString())).thenReturn(null);

        mockMvc.perform(get("/autos/"+automobile.getVin()))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateAuto_withObject_returnsAuto() throws Exception {
        automobile.setPrice(updateAutoRequest.getPrice());
        automobile.setPreowned(updateAutoRequest.getPreowned());
        automobile.setGrade(updateAutoRequest.getGrade());

        when(autosService.updateAuto(anyString(), anyInt(), any(Preowned.class), any(Grade.class))).thenReturn(automobile);

        mockMvc.perform(patch("/autos/"+automobile.getVin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateAutoRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("price").value(automobile.getPrice()))
                .andExpect(jsonPath("preowned").value(automobile.getPreowned().toString()))
                .andExpect(jsonPath("grade").value(automobile.getGrade().toString()));
    }

    @Test
    void updateAuto_notFound_returns204() throws Exception{
        when(autosService.updateAuto(anyString(), anyInt(), any(Preowned.class), any(Grade.class))).thenReturn(null);

        mockMvc.perform(patch("/autos/ABC123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateAutoRequest)))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateAuto_badRequest_returns400() throws Exception {
        when(autosService.updateAuto(anyString(), anyInt(), any(Preowned.class), any(Grade.class))).thenThrow(InvalidAutoException.class);

        mockMvc.perform(patch("/autos/BADVIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateAutoRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAuto_accepted_returns202() throws Exception {
        mockMvc.perform(delete("/autos/GOODVIN"))
                .andExpect(status().isAccepted());

        verify(autosService).deleteAuto(anyString());
    }

    @Test
    void deleteAuto_notFound_returns204() throws Exception{
        doThrow(InvalidAutoException.class).when(autosService).deleteAuto(anyString());

        mockMvc.perform(delete("/autos/BADVIN"))
                .andExpect(status().isNoContent());
    }
}
