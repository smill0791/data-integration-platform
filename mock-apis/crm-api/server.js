const express = require('express');
const cors = require('cors');
const { faker } = require('@faker-js/faker');

const app = express();
const PORT = 3001;

app.use(cors());
app.use(express.json());

// Generate 100 fake customers at startup
const customers = Array.from({ length: 100 }, (_, i) => ({
  id: `CRM-${String(i + 1).padStart(3, '0')}`,
  name: faker.person.fullName(),
  email: faker.internet.email().toLowerCase(),
  phone: faker.phone.number('###-###-####'),
  address: {
    street: faker.location.streetAddress(),
    city: faker.location.city(),
    state: faker.location.state({ abbreviated: true }),
    zipCode: faker.location.zipCode('#####'),
  },
  lastUpdated: faker.date.recent({ days: 30 }).toISOString(),
}));

console.log(`Generated ${customers.length} fake customers`);

// Middleware: random delay 100-500ms
function randomDelay(req, res, next) {
  const delay = Math.floor(Math.random() * 400) + 100;
  setTimeout(next, delay);
}

// Middleware: 5% random failure rate
function randomFailure(req, res, next) {
  if (Math.random() < 0.05) {
    return res.status(500).json({
      error: 'Internal Server Error',
      message: 'Simulated random failure',
    });
  }
  next();
}

app.use(randomDelay);
app.use(randomFailure);

// GET /api/customers - paginated list
app.get('/api/customers', (req, res) => {
  const page = parseInt(req.query.page) || 0;
  const size = parseInt(req.query.size) || 20;

  const start = page * size;
  const end = start + size;
  const content = customers.slice(start, end);
  const totalElements = customers.length;
  const totalPages = Math.ceil(totalElements / size);

  res.json({
    content,
    page,
    size,
    totalElements,
    totalPages,
  });
});

// GET /api/customers/:id - single customer
app.get('/api/customers/:id', (req, res) => {
  const customer = customers.find((c) => c.id === req.params.id);
  if (!customer) {
    return res.status(404).json({
      error: 'Not Found',
      message: `Customer ${req.params.id} not found`,
    });
  }
  res.json(customer);
});

app.listen(PORT, () => {
  console.log(`Mock CRM API running on http://localhost:${PORT}`);
  console.log(`Try: http://localhost:${PORT}/api/customers?page=0&size=20`);
});
