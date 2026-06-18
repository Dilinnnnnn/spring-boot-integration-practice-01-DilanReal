package ec.edu.epn.integration;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import ec.edu.epn.dto.AirportRequest;
import ec.edu.epn.repository.AirportRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class AirportControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AirportRepository airportRepository;

    @BeforeEach
    void setUp() {
        airportRepository.deleteAll();
    }

    @Test
    void shouldCreateAirport() throws Exception {
        AirportRequest airportrequest = new AirportRequest();
        airportrequest.setName("Mariscal Sucre");
        airportrequest.setCode("UIO");
        airportrequest.setCity("Quito");
        airportrequest.setCountry("Ecuador");

        mockMvc.perform(post("/api/airports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(airportrequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mariscal Sucre"))
                .andExpect(jsonPath("$.code").value("UIO"))
                .andExpect(jsonPath("$.city").value("Quito"))
                .andExpect(jsonPath("$.country").value("Ecuador"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void shouldRejectDuplicateAirportCode() throws Exception {
        AirportRequest request = createAirportRequest("Mariscal Sucre", "UIO", "Quito", "Ecuador");
        createAirport(request);

        // Intentar crear el mismo código de nuevo
        mockMvc.perform(post("/api/airports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllAirports() throws Exception {
        createAirport(createAirportRequest("Mariscal Sucre", "UIO", "Quito", "Ecuador"));
        createAirport(createAirportRequest("Guayaquil Airport", "GYE", "Guayaquil", "Ecuador"));

        mockMvc.perform(get("/api/airports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void shouldFindAirportById() throws Exception {
        AirportRequest request = createAirportRequest("Mariscal Sucre", "UIO", "Quito", "Ecuador");
        String response = createAirport(request);
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/airports/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.code").value("UIO"));
    }

    @Test
    void shouldFindAirportByCode() throws Exception {
        createAirport(createAirportRequest("Mariscal Sucre", "UIO", "Quito", "Ecuador"));

        mockMvc.perform(get("/api/airports/code/UIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("UIO"))
                .andExpect(jsonPath("$.name").value("Mariscal Sucre"));
    }

    @Test
    void shouldReturn404WhenAirportNotFound() throws Exception {
        mockMvc.perform(get("/api/airports/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectInvalidAirportRequest() throws Exception {
        AirportRequest request = new AirportRequest();
        request.setName(""); 
        request.setCode("UI");
        request.setCity("Quito");
        request.setCountry("Ecuador");

        mockMvc.perform(post("/api/airports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteAirport() throws Exception {
        AirportRequest airportrequest = new AirportRequest();
        airportrequest.setName("Santiago");
        airportrequest.setCode("SCL");
        airportrequest.setCity("Santiago");
        airportrequest.setCountry("Chile");

        String response = createAirport(airportrequest);

        Long id = objectMapper.readTree(response).get("id").asLong();
        mockMvc.perform(delete("/api/airports/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/airports/" + id))
                .andExpect(status().isNotFound());

    }

    @Test
    void shouldUpdateAirport() throws Exception {
        AirportRequest airportrequest = new AirportRequest();
        airportrequest.setName("Santiago");
        airportrequest.setCode("SCL");
        airportrequest.setCity("Santiago");
        airportrequest.setCountry("Chile");

        String response = createAirport(airportrequest);
        Long id = objectMapper.readTree(response).get("id").asLong();
        airportrequest.setName("Aeropuerto de Santiago de Chile");
        mockMvc.perform(put("/api/airports/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(airportrequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aeropuerto de Santiago de Chile"));

    }

    private AirportRequest createAirportRequest(String name, String code, String city, String country) {
        AirportRequest request = new AirportRequest();
        request.setName(name);
        request.setCode(code);
        request.setCity(city);
        request.setCountry(country);
        return request;
    }

    private String createAirport(AirportRequest airportRequest) throws Exception {
        return mockMvc.perform(post("/api/airports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(airportRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }
}