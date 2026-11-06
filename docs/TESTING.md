# Testing

Every demo needs at least 25 new unit tests, so every story comes with its tests in the same pull request.
CI runs all tests on every pull request and on every push to `main`; a red check blocks the merge.

## Frontend (Vitest + React Testing Library)

Run from the `frontend/` folder:

| Command | What it does |
|---|---|
| `npm test` | Runs all tests once (this is what CI runs) |
| `npm run test:watch` | Re-runs the affected tests on every save |
| `npm run test:coverage` | Runs all tests and writes a coverage report to `frontend/coverage/` (open `coverage/index.html`) |

Tests run in `jsdom`, a simulated browser. `src/test/setup.js` runs before every test file: it adds the
`jest-dom` matchers (`toBeInTheDocument()` and friends), and after each test it unmounts rendered components,
restores mocks and clears `localStorage`, so tests never leak into each other.

### Where tests live

- A test file sits next to the file it tests and has the same name with `.test`:
  `src/api/client.js` → `src/api/client.test.js`, `src/components/ProductCard.jsx` → `src/components/ProductCard.test.jsx`.
- Use `.test.jsx` when the test renders JSX, `.test.js` otherwise.
- Shared test helpers go in `src/test/`. Do not put production code there.
- One `describe` block per module or component; each `it` states one behaviour in plain English:
  `it('shows unreachable when the request fails')`.

### How to write them

- Test behaviour the user can see, not implementation details. Query by role or visible text
  (`getByRole('button', { name: 'Add to cart' })`, `findByText('Backend is up')`), not by CSS class.
- Use `await screen.findBy...` for anything that appears after a request or a state update.
- Never call the real backend. Mock `fetch` with `vi.spyOn(globalThis, 'fetch')`; see
  `src/api/client.test.js` and `src/components/BackendStatus.test.jsx` for examples.
- Components that use routing (`Link`, `useNavigate`) need a router in the test; wrap them in
  `<MemoryRouter initialEntries={['/some/path']}>` as in `src/App.test.jsx`.
- Use `@testing-library/user-event` for clicks and typing: `await userEvent.click(button)`.

## Backend (JUnit 5 + Mockito + MockMvc)

Run from the `backend/` folder: `mvn test` (CI runs `mvn -B verify`).

- Test classes mirror the package of the class they test under `src/test/java` and end in `Test`:
  `common/HealthController.java` → `common/HealthControllerTest.java`.
- Controllers: `@WebMvcTest` with `MockMvc`, mocking the services they call.
- Services: plain JUnit tests with Mockito mocks for repositories.
- Test method names describe the behaviour: `healthIsPublicAndReportsUp()`.

## Counting tests for the demo

We count one new `it(...)` (frontend) or `@Test` method (backend) as one unit test. Each one should check
real behaviour of our own code and be able to fail; a test that only exercises a library adds nothing.
