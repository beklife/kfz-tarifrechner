package de.musterversicherung.quote.api;

import de.musterversicherung.quote.service.QuoteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    // 200 statt 201: Wir speichern nichts. Es entsteht keine neue Ressource mit eigener URL.
    // POST statt GET: Die Eingabe ist ein JSON-Objekt im Body, kein einfacher URL-Parameter.
    @PostMapping
    public QuoteResponse createQuote(@Valid @RequestBody QuoteRequest request) {
        return QuoteResponse.from(quoteService.createQuote(request));
    }
}
