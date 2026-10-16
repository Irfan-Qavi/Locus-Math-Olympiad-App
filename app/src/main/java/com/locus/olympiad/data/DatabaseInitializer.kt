package com.locus.olympiad.data

import android.content.Context
import com.locus.olympiad.data.model.MathTopic
import com.locus.olympiad.data.model.PracticeQuestion
import com.locus.olympiad.data.model.TheoryModule
import com.locus.olympiad.data.model.UserTopicProfile

object DatabaseInitializer {

    suspend fun seedDatabase(context: Context? = null, database: LocusDatabase) {
        val profileDao = database.userTopicProfileDao()
        val questionDao = database.practiceQuestionDao()
        val theoryDao = database.theoryModuleDao()

        // 1. Seed initial 1000 Elo for 4 topics if not present
        if (profileDao.countProfiles() == 0) {
            val initialProfiles = MathTopic.ALL_TOPICS.map { topic ->
                UserTopicProfile(
                    category = topic.displayName,
                    eloRating = UserTopicProfile.DEFAULT_ELO // 1000
                )
            }
            profileDao.insertProfiles(initialProfiles)
        }

        // 2. Seed practice questions if table is empty
        if (questionDao.countQuestions() == 0) {
            val parsedQuestions = if (context != null) {
                SeedDataParser.parseSeedQuestions(context)
            } else {
                emptyList()
            }

            if (parsedQuestions.isNotEmpty()) {
                questionDao.insertQuestions(parsedQuestions)
            } else {
                questionDao.insertQuestions(getDefaultSampleQuestions())
            }
        }

        // 3. Seed Theory Modules if table is empty
        if (theoryDao.countModules() == 0) {
            theoryDao.insertModules(getDefaultSampleModules())
        }
    }

    private fun getDefaultSampleQuestions(): List<PracticeQuestion> {
        return listOf(
            PracticeQuestion(
                category = "Combinatorics",
                difficultyElo = 1000,
                latexContent = """Let $G = (V, E)$ be a connected simple graph with $n \ge 4$ vertices. Suppose for every partition $V = A \cup B$ of the vertex set into non-empty disjoint subsets, the number of crossing edges satisfies:
$$e(A, B) \ge \left\lceil \frac{|A| \cdot |B|}{n - 1} \right\rceil + \sum_{v \in A} \operatorname{deg}(v) \pmod 2$$
Determine the minimum possible chromatic number $\chi(G)$ across all such graphs.""",
                correctAnswer = "3",
                listOfHints = listOf(
                    "Consider the parity of the degree sum over $A$ and examine the case where $G$ contains an Eulerian sub-graph.",
                    "If $\\chi(G) = 2$, then $G$ is bipartite. Partition $V$ into the two color classes and check whether the parity constraint can hold for all cuts.",
                    "Notice that a cycle $C_5$ or $K_3$ requires at least 3 colors. Test if $\\chi(G)=3$ satisfies the lower bound by constructing an admissible 3-chromatic triangulation."
                ),
                sourceContest = "International Olympiad Training • Tier III",
                tier = "Tier III"
            ),
            PracticeQuestion(
                category = "Combinatorics",
                difficultyElo = 1080,
                latexContent = """Find the number of permutations $\pi$ of $\{1, 2, \dots, 6\}$ such that for all $1 \le k \le 5$:
$$|\pi(k) - \pi(k+1)| \ne 1$$""",
                correctAnswer = "90",
                listOfHints = listOf(
                    "Use the Principle of Inclusion-Exclusion (PIE) on the conditions that adjacent elements are consecutive.",
                    "Let $A_i$ be the property that $|\\pi(i) - \\pi(i+1)| = 1$. Count the intersections by grouping adjacent pairs into blocks.",
                    "Evaluate $\\sum_{k=0}^5 (-1)^k N(k)$ using the number of ways to place $k$ disjoint domino tiles."
                ),
                sourceContest = "IOQM Final Stage",
                tier = "Tier II"
            ),
            PracticeQuestion(
                category = "Algebra",
                difficultyElo = 1020,
                latexContent = """Find all real solutions $(x, y)$ to the symmetric system of equations:
$$\begin{cases} x^3 - y^3 = 7 \\ x^2 y - x y^2 = 2 \end{cases}$$
What is the maximum value of $x + y$?""",
                correctAnswer = "3",
                listOfHints = listOf(
                    "Notice that $x^3 - 3x^2y + 3xy^2 - y^3 = (x - y)^3$.",
                    "Compute $(x - y)^3 = (x^3 - y^3) - 3(x^2y - xy^2) = 7 - 3(2) = 1$. Thus $x - y = 1$.",
                    "Substitute $x = y + 1$ into $xy(x - y) = 2$ to find $y(y + 1) = 2$, giving roots $y = 1$ or $y = -2$."
                ),
                sourceContest = "RMO Algebra Round",
                tier = "Tier II"
            ),
            PracticeQuestion(
                category = "Algebra",
                difficultyElo = 1140,
                latexContent = """Let $P(x)$ be a polynomial with integer coefficients such that $P(1) = 2024$ and $P(2024) = 1$. 
If $P(k) = k$ for some integer $k$, find the value of $k$.""",
                correctAnswer = "No solution",
                listOfHints = listOf(
                    "Recall the fundamental integer polynomial divisibility property: $(a - b) \mid (P(a) - P(b))$ for distinct integers $a, b$.",
                    "Apply this property to pairs $(k, 1)$ and $(k, 2024)$.",
                    "Analyze whether $(k - 1) \mid (k - 2024)$ and $(k - 2024) \mid (k - 1)$ can both hold simultaneously."
                ),
                sourceContest = "IOQM National Camp",
                tier = "Tier III"
            ),
            PracticeQuestion(
                category = "Number Theory",
                difficultyElo = 990,
                latexContent = """Find the remainder when $2^{2026} + 3^{2026}$ is divided by $13$.""",
                correctAnswer = "0",
                listOfHints = listOf(
                    "Apply Fermat's Little Theorem: for prime $p=13$ and $\\gcd(a, 13) = 1$, $a^{12} \\equiv 1 \\pmod{13}$.",
                    "Reduce the exponent $2026$ modulo $12$: $2026 = 12 \\times 168 + 10$.",
                    "Calculate $2^{10} \\pmod{13}$ and $3^{10} \\pmod{13}$ and sum them up modulo 13."
                ),
                sourceContest = "IOQM Prelims",
                tier = "Tier I"
            ),
            PracticeQuestion(
                category = "Number Theory",
                difficultyElo = 1050,
                latexContent = """Find all pairs of primes $(p, q)$ such that:
$$p^2 - 2q^2 = 1$$
What is the sum $p + q$?""",
                correctAnswer = "5",
                listOfHints = listOf(
                    "Rewrite the equation as $p^2 - 1 = 2q^2$, which factors as $(p - 1)(p + 1) = 2q^2$.",
                    "Consider the parity of $p$. If $p=2$, $4 - 2q^2 = 1$ has no prime solution. Thus $p$ must be odd.",
                    "Since $p$ is odd, both $p - 1$ and $p + 1$ are consecutive even integers. Analyze the distribution of factors of $2q^2$ to deduce $q = 2$ and $p = 3$."
                ),
                sourceContest = "RMO Number Theory",
                tier = "Tier II"
            ),
            PracticeQuestion(
                category = "Geometry",
                difficultyElo = 1010,
                latexContent = """In acute triangle $\triangle ABC$, let $H$ be the orthocenter and $O$ the circumcenter. If $\angle A = 60^\circ$ and $BC = 12$, compute the length of the segment $OH$.""",
                correctAnswer = "12",
                listOfHints = listOf(
                    "Recall Euler's triangle formula or the relation between $AH$ and circumradius $R$: $AH = 2R \cos A$.",
                    "For $\angle A = 60^\circ$, $\cos 60^\circ = 1/2$, so $AH = R$.",
                    "Notice that in quadrilateral $AHCO$ or by the distance formula $OH^2 = 9R^2 - (a^2+b^2+c^2)$, what is $R$ when $a=12$ and $\angle A=60^\circ$? $a = 2R \sin 60^\circ$, giving $R = 4\sqrt{3}$."
                ),
                sourceContest = "RMO Geometry",
                tier = "Tier II"
            ),
            PracticeQuestion(
                category = "Geometry",
                difficultyElo = 1130,
                latexContent = """Let $\Gamma$ be the incircle of $\triangle ABC$ tangent to $BC, CA, AB$ at $D, E, F$ respectively. If $AB = 13$, $BC = 14$, $CA = 15$, compute the area of $\triangle DEF$.""",
                correctAnswer = "84/5",
                listOfHints = listOf(
                    "First compute the area and inradius of $\triangle ABC$ using Heron's formula: $s = 21, \Delta = 84, r = 4$.",
                    "Use contact triangle properties: $AE = AF = s - a = 7$, $BD = BF = s - b = 8$, $CD = CE = s - c = 6$.",
                    "Express Area($\triangle DEF$) in terms of $\triangle ABC$ by subtracting the outer vertex triangles or using the formula $\frac{r \cdot \text{Area}(\triangle ABC)}{2R}$."
                ),
                sourceContest = "IOQM National Level",
                tier = "Tier III"
            )
        )
    }

    private fun getDefaultSampleModules(): List<TheoryModule> {
        return listOf(
            TheoryModule(
                category = "Combinatorics",
                title = "Combinatorial Nullstellensatz & Graph Polynomials",
                chapterNumber = 4,
                readProgressPercent = 42,
                latexContent = """\section*{Alon's Combinatorial Nullstellensatz}
Let $\mathbb{F}$ be an arbitrary field, and let $P \in \mathbb{F}[x_1, \dots, x_n]$ be a polynomial of degree $\deg(P) = \sum_{i=1}^n t_i$, where each $t_i$ is a non-negative integer.

\begin{theorem}[Noga Alon, 1999]
If the coefficient of the monomial $\prod_{i=1}^n x_i^{t_i}$ in $P$ is non-zero, and if $S_1, \dots, S_n \subseteq \mathbb{F}$ are subsets of cardinality $|S_i| > t_i$ for all $i \in \{1, \dots, n\}$, then there exists a tuple:
$$(s_1, \dots, s_n) \in S_1 \times S_2 \times \cdots \times S_n$$
such that $P(s_1, \dots, s_n) \ne 0$.
\end{theorem}

\subsection*{Application to Graph Coloring}
Consider the graph polynomial $f_G(x) = \prod_{uv \in E, u < v} (x_u - x_v)$. If $G$ is $k$-choosable, we construct non-vanishing evaluations over list colorings $L(v)$ with $|L(v)| \ge k$."""
            ),
            TheoryModule(
                category = "Number Theory",
                title = "Diophantine Approximation & Dirichlet's Approximation Theorem",
                chapterNumber = 1,
                readProgressPercent = 57,
                latexContent = """\section*{Dirichlet's Approximation Theorem}
Let $\alpha$ be an arbitrary real number and let $N \ge 1$ be a positive integer.

\begin{theorem}
There exist integers $p$ and $q$ with $1 \le q \le N$ such that:
$$\left| \alpha - \frac{p}{q} \right| < \frac{1}{q N} \le \frac{1}{q^2}$$
Moreover, if $\alpha$ is irrational, there exist infinitely many pairs of coprime integers $(p, q)$ such that:
$$\left| \alpha - \frac{p}{q} \right| < \frac{1}{\sqrt{5} q^2}$$
The constant $\sqrt{5}$ is best possible (Hurwitz's Theorem, achieved by the golden ratio $\phi$).
\end{theorem}"""
            ),
            TheoryModule(
                category = "Algebra",
                title = "Symmetric Polynomials & Vieta's Jumping",
                chapterNumber = 2,
                readProgressPercent = 35,
                latexContent = """\section*{Vieta's Root Flipping (Vieta Jumping)}
Vieta jumping is a proof technique in Olympiad number theory and algebra, especially effective for Diophantine equations involving symmetric quadratic forms.

\begin{theorem}
Let $(x, y)$ be a solution to $x^2 - Axy + y^2 = B$. Fixing $y$, the equation is quadratic in $x$:
$$x^2 - (Ay)x + (y^2 - B) = 0$$
By Vieta's formulas, if $x_1$ is one root, the second root $x_2 = Ay - x_1 = \frac{y^2 - B}{x_1}$ yields a new integer solution.
\end{theorem}"""
            ),
            TheoryModule(
                category = "Geometry",
                title = "Barycentric Coordinates & Projective Geometry",
                chapterNumber = 3,
                readProgressPercent = 91,
                latexContent = """\section*{Homogeneous Barycentric Coordinates}
Let $\triangle ABC$ be a reference triangle. Any point $P$ in the plane can be uniquely represented up to scale by homogeneous coordinates $(u : v : w)$ where:
$$u = \frac{\operatorname{Area}(\triangle PBC)}{\operatorname{Area}(\triangle ABC)}, \quad v = \frac{\operatorname{Area}(\triangle PCA)}{\operatorname{Area}(\triangle ABC)}, \quad w = \frac{\operatorname{Area}(\triangle PAB)}{\operatorname{Area}(\triangle ABC)}$$
with $u + v + w = 1$. The incenter has coordinates $(a : b : c)$, and the circumcenter has coordinates $(\sin 2A : \sin 2B : \sin 2C)$."""
            )
        )
    }
}
