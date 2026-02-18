const express = require('express');
const cors = require('cors');
const { faker } = require('@faker-js/faker');

const app = express();
const PORT = 3002;

app.use(cors());
app.use(express.json());

const categories = ['Electronics', 'Furniture', 'Office Supplies', 'Software', 'Hardware', 'Networking'];
const warehouses = ['Warehouse-A', 'Warehouse-B', 'Warehouse-C', 'Warehouse-D'];

// Generate 80 fake products at startup
const products = Array.from({ length: 80 }, (_, i) => ({
  id: `ERP-${String(i + 1).padStart(3, '0')}`,
  sku: faker.string.alphanumeric({ length: 8 }).toUpperCase(),
  name: faker.commerce.productName(),
  description: faker.commerce.productDescription(),
  category: faker.helpers.arrayElement(categories),
  unitPrice: parseFloat(faker.commerce.price({ min: 5, max: 5000, dec: 2 })),
  quantity: faker.number.int({ min: 0, max: 500 }),
  warehouse: faker.helpers.arrayElement(warehouses),
}));

console.log(`Generated ${products.length} fake products`);

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

// GET /api/products - paginated list
app.get('/api/products', (req, res) => {
  const page = parseInt(req.query.page) || 0;
  const size = parseInt(req.query.size) || 20;

  const start = page * size;
  const end = start + size;
  const content = products.slice(start, end);
  const totalElements = products.length;
  const totalPages = Math.ceil(totalElements / size);

  res.json({
    content,
    page,
    size,
    totalElements,
    totalPages,
  });
});

// GET /api/products/:id - single product
app.get('/api/products/:id', (req, res) => {
  const product = products.find((p) => p.id === req.params.id);
  if (!product) {
    return res.status(404).json({
      error: 'Not Found',
      message: `Product ${req.params.id} not found`,
    });
  }
  res.json(product);
});

app.listen(PORT, () => {
  console.log(`Mock ERP API running on http://localhost:${PORT}`);
  console.log(`Try: http://localhost:${PORT}/api/products?page=0&size=20`);
});
